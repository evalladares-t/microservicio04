package com.nttdata.bootcamp.microservicio04.service.impl;

import com.nttdata.bootcamp.microservicio04.model.Account;
import com.nttdata.bootcamp.microservicio04.model.AccountType;
import com.nttdata.bootcamp.microservicio04.model.Credit;
import com.nttdata.bootcamp.microservicio04.model.Transaction;
import com.nttdata.bootcamp.microservicio04.model.dto.AccountUpdateDto;
import com.nttdata.bootcamp.microservicio04.model.dto.CreditUpdateDto;
import com.nttdata.bootcamp.microservicio04.repository.TransactionRepository;
import com.nttdata.bootcamp.microservicio04.service.TransactionService;
import com.nttdata.bootcamp.microservicio04.utils.constant.ErrorCode;
import com.nttdata.bootcamp.microservicio04.utils.exception.OperationNoCompletedException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  private TransactionRepository transactionRepository;
  private WebClient webClientAccount;
  private WebClient webClientCredit;

  public TransactionServiceImpl(
      TransactionRepository transactionRepository, WebClient webClientAccount, WebClient webClientCredit) {
    this.transactionRepository = transactionRepository;
    this.webClientAccount = webClientAccount;
    this.webClientCredit = webClientCredit;
  }

  @Override
  public Mono<Transaction> create(Transaction transaction) {

    if (transaction.getAmount().compareTo(BigDecimal.ZERO) == 0) {
      log.warn("Transaction amount must be different from zero");
      return Mono.empty();
    }

    Map<String, Function<Transaction, Flux<Transaction>>> productProcessors =
        Map.of(
            "accountId",
            tx -> createTransactionAccount(tx, tx.getAccountId()),
            "creditId",
            tx -> createTransactionCredit(tx, tx.getCreditId()));
    List<Map.Entry<String, Function<Transaction, Flux<Transaction>>>> validProducts =
        productProcessors.entrySet().stream()
            .filter(entry -> isFieldNonNull(transaction, entry.getKey()))
            .toList();

    if (validProducts.size() != 1) {
      return Mono.error(new IllegalArgumentException("Exactly one product must be non-null"));
    }

    return validProducts.get(0).getValue().apply(transaction).next();
  }

  private Flux<Transaction> createTransactionAccount(Transaction transaction, String accountId) {
    return findAccount(accountId)
        .flatMap(
            account -> {
              setDefaultTransactionProperties(transaction, account);
              return validateTransactionWithAccount(account, transaction);
            })
        // .switchIfEmpty(findByCreditIdService(creditID));
        .doOnError(e -> log.error("Error creating transaction: ", e));
  }

  private Flux<Transaction> createTransactionCredit(Transaction transaction, String creditId) {
    return findCredit(creditId)
        .flatMap(
            credit -> {
              updateCreditAmount(
                      credit.getId(), credit.getAmountAvailable().add(transaction.getAmount()));
              return transactionRepository.insert(transaction);
            })
        // .switchIfEmpty(findByCreditIdService(creditID));
        .doOnError(e -> log.error("Error creating transaction: ", e));
  }

  private Mono<Boolean> isTransactionAccountAvailable(Integer transactionLimit) {
    YearMonth mesAnioActual = YearMonth.now();

    // Definir el rango para el primer y último día del mes actual
    LocalDateTime start = mesAnioActual.atDay(1).atStartOfDay();
    LocalDateTime end = mesAnioActual.atEndOfMonth().atTime(23, 59, 59);

    // Contar documentos en el rango del mes y año actuales
    return transactionRepository
        .countByCreatedBetween(start, end)
        .map(count -> transactionLimit > count);
  }

  private Mono<Boolean> isDateTransactionAccountAvailable(Integer dateAllowedTransaction) {
    LocalDate dateCompletedNow = LocalDate.now();
    return Mono.just(dateCompletedNow.getDayOfMonth() == dateAllowedTransaction);
  }

  private Mono<Transaction> validateTransactionWithCredit(Credit credit, Transaction transaction) {
    if (credit.getActive()
        && ((credit.getAmountAvailable().add(transaction.getAmount())).compareTo(BigDecimal.ZERO)
            >= 0)) {
      updateCreditAmount(
              credit.getId(), credit.getAmountAvailable().add(transaction.getAmount()));
      return transactionRepository.insert(transaction);

    }
    return Mono.just(new Transaction());
  }

  private Mono<Transaction> validateTransactionWithAccount(
      Account account, Transaction transaction) {
    String accountType = account.getAccountType().getDescription();
    if (account.getActive()
        && ((account.getAmountAvailable().add(transaction.getAmount())).compareTo(BigDecimal.ZERO)
            >= 0)) {
      // Si la cuenta es de ahorro
      if (AccountType.SAVING.getDescription().equals(accountType)) {
        return isTransactionAccountAvailable(account.getTransactionLimit())
            .flatMap(
                allowed -> {
                  if (allowed) {
                    updateAccountAmount(
                        account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
                    return transactionRepository.insert(transaction);
                  } else {
                    log.warn("Transaction not allowed for this account");
                    return Mono.empty();
                  }
                });
      } else if (AccountType.CURRENT.getDescription().equals(accountType)) {
        updateAccountAmount(
            account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
        return transactionRepository.insert(transaction);
      } else {
        return isTransactionAccountAvailable(1)
            .flatMap(
                allowed -> {
                  if (allowed) {
                    return isDateTransactionAccountAvailable(account.getDateAllowedTransaction())
                        .flatMap(
                            allowedDate -> {
                              if (allowedDate) {
                                updateAccountAmount(
                                    account.getId(),
                                    account.getAmountAvailable().add(transaction.getAmount()));
                                return transactionRepository.insert(transaction);
                              } else {
                                log.warn("Transaction not allowed for this account");
                                return Mono.empty();
                              }
                            });
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
    updateByAccountId(accountId, accountUpdateDto).subscribe();
  }

  public void updateCreditAmount(String creditId, BigDecimal ammount) {
    CreditUpdateDto creditUpdateDto = new CreditUpdateDto();
    creditUpdateDto.setAmountAvailable(ammount);
    updateByCreditId(creditId, creditUpdateDto).subscribe();
  }

  private void setDefaultTransactionProperties(Transaction transaction, Account account) {
    transaction.setAccountId(account.getId());
    transaction.setCreated(LocalDate.now());
    transaction.setActive(true);
  }

  @Override
  public Mono<Transaction> findById(String transactionId) {
    return transactionRepository.findById(transactionId);
  }

  @Override
  public Flux<Transaction> findAll() {
    return transactionRepository.findAll();
  }

  @Override
  public Mono<Transaction> update(Transaction transaction, String transactionId) {
    log.info("Update a account in the service.");
    return transactionRepository
        .findById(transactionId)
        .flatMap(
            customerDB -> {
              transaction.setId(customerDB.getId());
              return transactionRepository.save(transaction);
            })
        .switchIfEmpty(
            Mono.error(
                new OperationNoCompletedException(
                    ErrorCode.TRANSACTION_NO_UPDATE.getCode(),
                    ErrorCode.TRANSACTION_NO_UPDATE.getMessage())));
  }

  @Override
  public Mono<Transaction> change(Transaction transaction, String transactionId) {
    return transactionRepository
        .findById(transactionId)
        .flatMap(
            entidadExistente -> {
              // Iterar sobre los campos del objeto entidadExistente
              Field[] fields = transaction.getClass().getDeclaredFields();
              for (Field field : fields) {
                if ("id".equals(field.getName())) {
                  continue; // Saltar el campo 'id'
                }
                field.setAccessible(true); // Para acceder a campos privados
                try {
                  // Verificar si el valor del campo en entidadParcial no es null
                  Object value = field.get(transaction);
                  if (value != null) {
                    // Actualizar el campo correspondiente en entidadExistente
                    ReflectionUtils.setField(field, entidadExistente, value);
                  }
                } catch (IllegalAccessException e) {
                  e.printStackTrace(); // Manejo de errores si hay problemas con la reflexión
                }
              }
              // Guardar la entidad modificada
              return transactionRepository.save(entidadExistente);
            })
        .switchIfEmpty(
            Mono.error(
                new OperationNoCompletedException(
                    ErrorCode.TRANSACTION_NO_UPDATE.getCode(),
                    ErrorCode.TRANSACTION_NO_UPDATE.getMessage())));
  }

  @Override
  public Mono<Transaction> remove(String transactionId) {
    return transactionRepository
        .findById(transactionId)
        .flatMap(p -> transactionRepository.deleteById(p.getId()).thenReturn(p));
  }

  @Override
  public Flux<Transaction> findByAccountId(String id) {
    return transactionRepository.findByAccountId(id);
  }

  @Override
  public Flux<Transaction> findByCreditId(String id) {
    return transactionRepository.findByCreditId(id);
  }

  private boolean isFieldNonNull(Transaction transaction, String fieldName) {
    try {
      Field field = Transaction.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(transaction) != null;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Error accessing field: " + fieldName, e);
    }
  }

  public Flux<Account> findAccount(String id) {
    return this.webClientAccount
        .get()
        .uri(uriBuilder -> uriBuilder.path("v1/account/" + id).build())
        .retrieve()
        .bodyToFlux(Account.class);
  }

  public Flux<Credit> findCredit(String id) {
    return this.webClientCredit
        .get()
        .uri(uriBuilder -> uriBuilder.path("v1/credit/" + id).build())
        .retrieve()
        .bodyToFlux(Credit.class);
  }

  public Mono<Account> updateByAccountId(String id, AccountUpdateDto accountUpdateDto) {
    log.info("Update account with id: [{}]", id);
    return this.webClientAccount
            .patch()
            .uri(uriBuilder -> uriBuilder.path("v1/account/" + id).build())
            .bodyValue(accountUpdateDto)
            .retrieve()
            .bodyToMono(Account.class);
  }

  public Mono<Credit> updateByCreditId(String id, CreditUpdateDto creditUpdateDto) {
    log.info("Update account with id: [{}]", id);
    return this.webClientCredit
            .patch()
            .uri(uriBuilder -> uriBuilder.path("v1/credit/" + id).build())
            .bodyValue(creditUpdateDto)
            .retrieve()
            .bodyToMono(Credit.class);
  }
}
