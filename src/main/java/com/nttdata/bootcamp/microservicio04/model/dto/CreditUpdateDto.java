package com.nttdata.bootcamp.microservicio04.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditUpdateDto {
  private BigDecimal amountAvailable;
}
