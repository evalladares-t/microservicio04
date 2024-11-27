package com.nttdata.bootcamp.microservicio04.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transaction")
public class Transaction {
  @Id private String id = UUID.randomUUID().toString();
  private BigDecimal amount;
  private LocalDate created;
  private TransactionType transactionType;
  private String accountId;
  private String destinationAccountId;
  private Boolean ownerTransaction;
  private String creditId;
  private Boolean active;
}
