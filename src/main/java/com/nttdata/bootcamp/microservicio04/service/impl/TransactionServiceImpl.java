package com.nttdata.bootcamp.microservicio04.service.impl;

import com.nttdata.bootcamp.microservicio04.model.Account;
import com.nttdata.bootcamp.microservicio04.model.AccountType;
import com.nttdata.bootcamp.microservicio04.model.Credit;
import com.nttdata.bootcamp.microservicio04.model.Transaction;
import com.nttdata.bootcamp.microservicio04.model.TransactionType;
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
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
      TransactionRepository transactionRepository,
      WebClient webClientAccount,
      WebClient webClientCredit) {
    this.transactionRepository = transactionRepository;
    this.webClientAccount = webClientAccount;
    this.webClientCredit = webClientCredit;
  }

  @Override
  public Mono<Transaction> create(Transaction transaction) {

    if (transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
      transactionNotAllowed(ErrorCode.TRANSACTION_AMOUNT_NOT_ALLOWED);
    }

    Map<String, Function<Transaction, Flux<Transaction>>> productProcessors =
        Map.of(
            "accountId",
            tx -> createTransactionAccount(tx),
            "creditId",
            tx -> createTransactionCredit(tx));
    List<Map.Entry<String, Function<Transaction, Flux<Transaction>>>> validProducts =
        productProcessors.entrySet().stream()
            .filter(entry -> isFieldNonNull(transaction, entry.getKey()))
            .toList();

    if (validProducts.size() != 1) {
      return Mono.error(new IllegalArgumentException("Exactly one product must be non-null"));
    }

    return validProducts.get(0).getValue().apply(transaction).next();
  }

  private Flux<Transaction> createTransactionAccount(Transaction transaction) {
    return findAccount(transaction.getAccountId())
        .flatMap(account -> handleTransactionByType(transaction, account))
        .doOnError(e -> log.error("Error creating transaction: ", e));
  }

  private Flux<Transaction> handleTransactionByType(Transaction transaction, Account account) {
    setDefaultTransactionProperties(transaction, account);

    if (TransactionType.BANK_TRANSFER.equals(transaction.getTransactionType())) {
      return handleBankTransfer(transaction, account);
    }

    return validateTransactionWithAccount(account, transaction).flux();
  }

  private Flux<Transaction> handleBankTransfer(Transaction transaction, Account originAccount) {

    transaction.setAmount(transaction.getAmount().negate());
    Mono<Transaction> originValidation = validateTransactionWithAccount(originAccount, transaction);

    return findAccount(transaction.getDestinationAccountId())
        .flatMap(
            destinationAccount -> {
              Transaction destinationTransaction = createDestinationTransaction(transaction);
              updateAccountAmount(
                  destinationAccount.getId(),
                  destinationAccount.getAmountAvailable().add(destinationTransaction.getAmount()));
              Mono<Transaction> destinationValidation =
                  transactionRepository.insert(destinationTransaction);

              return Flux.zip(originValidation, destinationValidation)
                  .flatMap(tuple -> Flux.just(transaction, destinationTransaction))
                  .switchIfEmpty(transactionNotAllowed(ErrorCode.TRANSACTION_TYPE_NO_ALLOWED));
            });
  }

  private Mono<Transaction> transactionNotAllowed(ErrorCode errorCode) {
    log.warn("Account type not allowed for this customer");
    return Mono.error(
        new OperationNoCompletedException(errorCode.getCode(), errorCode.getMessage()));
  }

  private Flux<Transaction> createTransactionCredit(Transaction transaction) {
    return findCredit(transaction.getCreditId())
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

    // Contar documentos en el rango del mes y año actuales y que se dueño de la transaccion
    return transactionRepository
        .countByCreatedBetweenAndOwnerTransactionIsTrue(start, end)
        .map(count -> transactionLimit > count);
  }

  private Mono<Boolean> isDateTransactionAccountAvailable(Integer dateAllowedTransaction) {
    LocalDate dateCompletedNow = LocalDate.now();
    return Mono.just(dateCompletedNow.getDayOfMonth() == dateAllowedTransaction);
  }

  private Mono<Transaction> validateTransactionWithAccount(
      Account account, Transaction transaction) {
    if (!account.getActive()) {
      log.warn("Account is not active");
      return Mono.empty();
    }

    if (!isTransactionAmountValid(account, transaction)) {
      log.warn("Insufficient funds for transaction");
      return Mono.empty();
    }

    if (AccountType.SAVING.equals(account.getAccountType())) {
      return validateSavingAccount(account, transaction);
    } else if (AccountType.CURRENT
        .getDescription()
        .equals(account.getAccountType().getDescription())) {
      return processTransaction(account, transaction);
    } else {
      return validateOtherAccountTypes(account, transaction);
    }
  }

  private boolean isTransactionAmountValid(Account account, Transaction transaction) {
    return account.getAmountAvailable().add(transaction.getAmount()).compareTo(BigDecimal.ZERO)
        >= 0;
  }

  private Mono<Transaction> validateSavingAccount(Account account, Transaction transaction) {
    return isTransactionAccountAvailable(account.getTransactionLimit())
        .flatMap(
            allowed -> {
              if (allowed) {
                return processTransaction(account, transaction);
              } else {
                log.error("The transaction limit per month was exceeded");
                return transactionNotAllowed(ErrorCode.TRANSACTION_LIMIT_EXCEEDED);
              }
            });
  }

  private Mono<Transaction> validateOtherAccountTypes(Account account, Transaction transaction) {
    return isTransactionAccountAvailable(1)
        .flatMap(
            allowed -> {
              if (!allowed) {
                log.warn("Transaction not allowed for this account");
                return Mono.empty();
              }

              return isDateTransactionAccountAvailable(account.getDateAllowedTransaction())
                  .flatMap(
                      allowedDate -> {
                        if (!allowedDate) {
                          log.warn("Transaction not allowed due to date restrictions");
                          return Mono.empty();
                        }

                        return processTransaction(account, transaction);
                      });
            });
  }

  private Mono<Transaction> processTransaction(Account account, Transaction transaction) {
    updateAccountAmount(account.getId(), account.getAmountAvailable().add(transaction.getAmount()));
    return transactionRepository.insert(transaction);
  }

  private Transaction createDestinationTransaction(Transaction transaction) {
    Transaction destinationTransaction = new Transaction();
    BeanUtils.copyProperties(transaction, destinationTransaction);
    destinationTransaction.setId(UUID.randomUUID().toString());
    destinationTransaction.setAccountId(transaction.getDestinationAccountId());
    destinationTransaction.setAmount(transaction.getAmount().abs());
    destinationTransaction.setDestinationAccountId(transaction.getAccountId());
    destinationTransaction.setOwnerTransaction(false);
    return destinationTransaction;
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
    transaction.setOwnerTransaction(true);
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
        .uri(uriBuilder -> uriBuilder.path("v1/accounts/" + id).build())
        .retrieve()
        .bodyToFlux(Account.class);
  }

  public Flux<Credit> findCredit(String id) {
    return this.webClientCredit
        .get()
        .uri(uriBuilder -> uriBuilder.path("v1/credits/" + id).build())
        .retrieve()
        .bodyToFlux(Credit.class);
  }

  public Mono<Account> updateByAccountId(String id, AccountUpdateDto accountUpdateDto) {
    log.info("Update account with id: [{}]", id);
    return this.webClientAccount
        .patch()
        .uri(uriBuilder -> uriBuilder.path("v1/accounts/" + id).build())
        .bodyValue(accountUpdateDto)
        .retrieve()
        .bodyToMono(Account.class);
  }

  public Mono<Credit> updateByCreditId(String id, CreditUpdateDto creditUpdateDto) {
    log.info("Update account with id: [{}]", id);
    return this.webClientCredit
        .patch()
        .uri(uriBuilder -> uriBuilder.path("v1/credits/" + id).build())
        .bodyValue(creditUpdateDto)
        .retrieve()
        .bodyToMono(Credit.class);
  }
}
