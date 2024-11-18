package com.nttdata.bootcamp.microservicio04.config;

import com.nttdata.bootcamp.microservicio04.utils.constant.ErrorCode;
import com.nttdata.bootcamp.microservicio04.utils.exception.OperationNoCompletedException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OperationNoCompletedException.class)
  public Mono<ResponseEntity<Map<String, String>>> handleCustomException(
      OperationNoCompletedException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("errorCode", ex.getErrorCode());
    errorResponse.put("errorMessage", ex.getErrorMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Map<String, String>>> handleGenericException(Exception ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    errorResponse.put("errorMessage", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
  }
}
