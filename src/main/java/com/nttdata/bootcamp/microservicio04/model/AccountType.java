package com.nttdata.bootcamp.microservicio04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
  AHORRO("001", "SavingAccount"),
  CORRIENTE("002", "CurrentAccount"),
  PLAZO_FIJO("003", "FixedTerm");

  private final String code;
  private final String description;

}