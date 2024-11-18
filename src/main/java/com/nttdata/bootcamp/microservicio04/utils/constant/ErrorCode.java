package com.nttdata.bootcamp.microservicio04.utils.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  DATA_NOT_FOUND("404", "Data not found"),
  INVALID_REQUEST("400", "Invalid request parameters"),
  TRANSACTION_NO_CREATED("404", "The transaction was not created"),
  TRANSACTION_TYPE_ALREADY("404", "The client already has an transaction of this type"),
  TRANSACTION_NO_UPDATE("404", "The transaction was not update"),
  TRANSACTION_NO_DELETED("404", "The transaction was not deleted"),
  TRANSACTION_NO_COMPLETED("404", "Operación no completada"),
  TRANSACTION_TYPE_NO_ALLOWED("404", "Account type not allowed for this customer"),

  INTERNAL_SERVER_ERROR("500", "Internal server error"),
  SERVICE_UNAVAILABLE("503", "Service unavailable");

  private final String code;
  private final String message;
}
