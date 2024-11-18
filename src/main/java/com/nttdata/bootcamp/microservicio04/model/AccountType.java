package com.nttdata.bootcamp.microservicio04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
  SAVING("001", "AHORRO"),
  CURRENT("002", "CORRIENTE"),
  FIXED_TERM("003", "PLAZO FIJO");

  private final String code;
  private final String description;
}
