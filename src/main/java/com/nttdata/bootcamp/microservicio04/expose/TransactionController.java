package com.nttdata.bootcamp.microservicio04.expose;

import com.nttdata.bootcamp.microservicio04.model.Transaction;
import com.nttdata.bootcamp.microservicio04.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("api/v1/transactions")
public class TransactionController {

  private TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping({"/{id}/", "/{id}"})
  public Mono<Transaction> findbyId(@PathVariable("id") String id) {
    log.info("Find by id a account in the controller.");
    return transactionService.findById(id);
  }

  @GetMapping({"", "/"})
  public Flux<Transaction> findAll() {
    log.info("List all accounts in the controller.");
    return transactionService.findAll();
  }

  @PostMapping("/")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Transaction> create(@RequestBody Transaction transaction) {
    log.info("Create a transaction in the controller.");
    return transactionService.create(transaction);
  }

  @PutMapping({"/{id}/", "/{id}"})
  public Mono<ResponseEntity<Transaction>> update(
      @RequestBody Transaction account, @PathVariable("id") String transactionId) {
    log.info("Update an account in the controller.");
    return transactionService
        .update(account, transactionId)
        .flatMap(accountUpdate -> Mono.just(ResponseEntity.ok(accountUpdate)))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @PatchMapping({"/{id}/", "/{id}"})
  public Mono<ResponseEntity<Transaction>> change(
      @RequestBody Transaction account, @PathVariable("id") String transactionId) {
    log.info("Change an account in the controller.");
    return transactionService
        .change(account, transactionId)
        .flatMap(accountUpdate -> Mono.just(ResponseEntity.ok(accountUpdate)))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @DeleteMapping({"/{id}/", "/{id}"})
  public Mono<ResponseEntity<Transaction>> delete(@PathVariable("id") String transactionId) {
    log.info("Delete an account in the controller.");
    return transactionService
        .remove(transactionId)
        .flatMap(account -> Mono.just(ResponseEntity.ok(account)))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @GetMapping({"/account/{id}/", "/account/{id}"})
  public Flux<Transaction> findByAccountId(@PathVariable("id") String transactionId) {
    log.info("Find an accounts by customerId in the controller.");
    return transactionService.findByAccountId(transactionId);
  }

  @GetMapping({"/credit/{id}/", "/credit/{id}"})
  public Flux<Transaction> findByCreditId(@PathVariable("id") String transactionId) {
    log.info("Find an credit by customerId in the controller.");
    return transactionService.findByCreditId(transactionId);
  }
}
