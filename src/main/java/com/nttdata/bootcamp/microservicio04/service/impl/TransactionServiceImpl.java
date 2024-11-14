package com.nttdata.bootcamp.microservicio04.service.impl;

import com.nttdata.bootcamp.microservicio04.model.Account;
import com.nttdata.bootcamp.microservicio04.model.AccountType;
import com.nttdata.bootcamp.microservicio04.model.Transaction;
import com.nttdata.bootcamp.microservicio04.model.dto.AccountUpdateDto;
import com.nttdata.bootcamp.microservicio04.repository.TransactionRepository;
import com.nttdata.bootcamp.microservicio04.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  private TransactionRepository transactionRepository;

  private WebClient webClientConfig;

  public TransactionServiceImpl (TransactionRepository transactionRepository, WebClient webClientConfig) {
    this.transactionRepository = transactionRepository;
    this.webClientConfig = webClientConfig;
  }
  @Override
  public Mono<Transaction> create(Transaction transaction) {
    String accountId = transaction.getAccountId();
    //String creditId = transaction.getCreditId();

    if (accountId.isBlank()) {
      log.warn("Account ID or Credit ID is empty");
      return Mono.empty();
    }

    return findByAccountID(accountId)
            .flatMap(account -> {
              transaction.setAccountId(account.getId());
              return validateTransactionWithAccount(account, transaction);
            })
            //.switchIfEmpty(findByCreditIDService(creditID));
            .doOnError(e -> log.error("Error creating transaction: ", e));
  }

  private Mono<Boolean>  isTransactionAccountAvailable (Integer transactionLimit) {
    YearMonth mesAnioActual = YearMonth.now();

    // Definir el rango para el primer y último día del mes actual
    LocalDateTime start = mesAnioActual.atDay(1).atStartOfDay();
    LocalDateTime end = mesAnioActual.atEndOfMonth().atTime(23, 59, 59);

    // Contar documentos en el rango del mes y año actuales
    return transactionRepository.countByDateBetween(start, end).map(count -> transactionLimit > count);
  }

  private Mono<Transaction> validateTransactionWithAccount(Account account, Transaction transaction) {
    String accountType = account.getAccountType().getDescription();
    if(account.getActive() && ((account.getAmountAvailable().add(transaction.getAmount())).compareTo(BigDecimal.ZERO) >= 0)){
      // Si la cuenta es de ahorro
      if (AccountType.AHORRO.getDescription().equals(accountType)) {
        return isTransactionAccountAvailable(account.getTransactionLimit())
                .flatMap(allowed -> {
                  if (allowed) {
                    updateAccountAmount(account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
                    return transactionRepository.insert(transaction);
                  } else {
                    log.warn("Transaction not allowed for this account");
                    return Mono.empty();
                  }
                });
      } else if (AccountType.CORRIENTE.getDescription().equals(accountType)){
        updateAccountAmount(account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
        return transactionRepository.insert(transaction);
      }
      else  {
        return isTransactionAccountAvailable(1)
                .flatMap(allowed -> {
                  if (allowed) {
                    updateAccountAmount(account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
                    return transactionRepository.insert(transaction);
                  } else {
                    log.warn("Transaction not allowed for this account");
                    return Mono.empty();
                  }
                });
      }

    }
    return Mono.empty();
  }

  public void updateAccountAmount(String accountId, BigDecimal ammount) {
    AccountUpdateDto accountUpdateDto = new AccountUpdateDto();
    accountUpdateDto.setAmountAvailable(ammount);
    updateByAccountID(accountId, accountUpdateDto).subscribe();
  }
  public Mono<Account> findByAccountID(String id) {
    log.info("Getting client with id: [{}]", id);
    return this.webClientConfig.get()
            .uri(uriBuilder -> uriBuilder
                    .path("v1/account/"+ id)
                    .build())
            .retrieve()
            .bodyToMono(Account.class);
  }

  public Mono<Account> updateByAccountID(String id, AccountUpdateDto accountUpdateDto) {
    log.info("Update account with id: [{}]", id);
    return this.webClientConfig.patch()
            .uri(uriBuilder -> uriBuilder
                    .path("v1/account/"+ id)
                    .build())
            .bodyValue(accountUpdateDto)
            .retrieve()
            .bodyToMono(Account.class);
  }

  public Mono<Account> findByCreditIDService(String id) {
    log.info("Getting client with id: [{}]", id);
    return this.webClientConfig.get()
            .uri(uriBuilder -> uriBuilder
                    .path("v1/credit/"+ id)
                    .build())
            .retrieve()
            .bodyToMono(Account.class);
  }
}
