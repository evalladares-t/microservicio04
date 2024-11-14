package com.nttdata.bootcamp.microservicio04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransactionType {
  RETIRO("001", "RETIRO"),
  DEPOSITO("002", "DEPOSITO"),
  TRANSFERENCIA_BANCARIA("003", "TRANSFERENCIA_BANCARIA"),
  TRANSFERENCIA_INTERBANCARIA("004", "TRANSFERENCIA_INTERBANCARIA");

  private final String code;
  private final String description;

}