package com.nttdata.bootcamp.microservicio04.repository;

import com.nttdata.bootcamp.microservicio04.model.Transaction;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

  Mono<Long> countByCreatedBetween(LocalDateTime start, LocalDateTime end);
}
