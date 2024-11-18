package com.nttdata.bootcamp.microservicio04.utils.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OperationNoCompletedException extends RuntimeException {

  private final String errorCode;
  private final String errorMessage;
}
