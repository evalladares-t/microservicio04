package com.nttdata.bootcamp.microservicio04.service;

import com.nttdata.bootcamp.microservicio04.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {

  Mono<Transaction> create(Transaction transaction);

  Mono<Transaction> findById(String transactionId);

  Flux<Transaction> findAll();

  Mono<Transaction> update(Transaction transaction, String transactionId);

  Mono<Transaction> change(Transaction transaction, String transactionId);

  Mono<Transaction> remove(String transactionId);
}
