package com.nttdata.bootcamp.microservicio04.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Customer {

  private String id;
  private String customerType;
  private String firstName;
  private String lastName;
  private DocumentIdentity documentIdentity;
  private boolean isActive;

}