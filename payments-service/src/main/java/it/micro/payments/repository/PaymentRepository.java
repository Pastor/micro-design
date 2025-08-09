package it.micro.payments.repository;

import it.micro.payments.model.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    Flux<Payment> findByStatus(String status);

    Mono<Long> countByStatus(String status);
}
