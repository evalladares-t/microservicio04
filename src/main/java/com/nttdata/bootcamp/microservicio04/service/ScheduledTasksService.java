package com.nttdata.bootcamp.microservicio04.service;

import com.nttdata.bootcamp.microservicio04.model.Account;
import com.nttdata.bootcamp.microservicio04.model.AccountType;
import com.nttdata.bootcamp.microservicio04.model.Transaction;
import com.nttdata.bootcamp.microservicio04.model.TransactionType;
import com.nttdata.bootcamp.microservicio04.model.dto.AccountUpdateDto;
import com.nttdata.bootcamp.microservicio04.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@EnableScheduling
@Slf4j
public class ScheduledTasksService {

  private final WebClient webClientAccount;

  private final TransactionRepository transactionRepository;

  public ScheduledTasksService(TransactionRepository transactionRepository, WebClient webClientAccount) {
    this.webClientAccount = webClientAccount;
    this.transactionRepository = transactionRepository;
  }

  // @Scheduled(cron = "*/5 * * * * *") // Cada 5 segundos para pruebas
  @Scheduled(cron = "0 0 0 L * ?") // Último día del mes a medianoche
  public void applyMaintenanceFees() {
    findAllAccount()
        .filter(account -> account.getAccountType().equals(AccountType.CURRENT))
        .filter(Account::getActive)
        .flatMap(this::applyFeeToAccount) // Paso 2: Procesar cada cuenta
        .then()
        .subscribe(
            null,
            error ->
                System.out.println("Error when making maintenance payment: " + error.getMessage()),
            () -> System.out.println("Commission collection completed."));
  }

  private Mono<Transaction> applyFeeToAccount(Account account) {
    // Paso 2.1: Crear transacción de mantenimiento
    Transaction transaction = new Transaction();
    transaction.setAccountId(account.getId());
    transaction.setTransactionType(TransactionType.MAINTENANCE_PAYMENT);
    transaction.setAmount(account.getCommissionRate());
    transaction.setCreated(LocalDate.now());
    transaction.setActive(true);
    // Paso 2.2: Calcular nuevo saldo
    BigDecimal newBalance = account.getAmountAvailable().subtract(account.getCommissionRate());
    // Paso 2.3: Registrar transacción y actualizar saldo
    updateByAccountId(account.getId(), newBalance).subscribe();
    return transactionRepository.save(transaction);
  }

  public Flux<Account> findAllAccount() {
    log.info("Getting client with id");
    return this.webClientAccount
        .get()
        .uri(uriBuilder -> uriBuilder.path("v1/accounts/").build())
        .retrieve()
        .bodyToFlux(Account.class);
  }

  public Flux<Account> updateByAccountId(String accountId, BigDecimal newBalance) {
    log.info("Getting client with id");
    return this.webClientAccount
        .patch()
        .uri(uriBuilder -> uriBuilder.path("v1/accounts/" + accountId).build())
        .bodyValue(new AccountUpdateDto(newBalance))
        .retrieve()
        .bodyToFlux(Account.class);
  }
}
