package com.nttdata.bootcamp.microservicio04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransactionType {
  WITHDRAWAL("001", "RETIRO"),
  DEPOSIT("002", "DEPOSITO"),
  BANK_TRANSFER("003", "TRANSFERENCIA BANCARIA"),
  INTERBANK_TRANSFER("004", "TRANSFERENCIA INTERBANCARIA"),
  MAINTENANCE_PAYMENT("004", "PAGO DE MANTENIMIENTO");

  private final String code;
  private final String description;
}
