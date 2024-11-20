package com.nttdata.bootcamp.microservicio04.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Credit {

  private String id;
  private String customerId;
  private String currency;
  private BigDecimal creditLimit;
  private BigDecimal amountAvailable;
  private Boolean active;
}
