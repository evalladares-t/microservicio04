package com.nttdata.bootcamp.microservicio04.service;

import com.nttdata.bootcamp.microservicio04.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionService {

  Mono<Transaction> create(Transaction transaction);

}
