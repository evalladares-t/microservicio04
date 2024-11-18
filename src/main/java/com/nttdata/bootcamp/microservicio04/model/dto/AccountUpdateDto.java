package com.nttdata.bootcamp.microservicio04.model.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateDto {
  private BigDecimal amountAvailable;
}
