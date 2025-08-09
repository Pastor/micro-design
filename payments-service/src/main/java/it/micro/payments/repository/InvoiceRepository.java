package it.micro.payments.repository;

import it.micro.payments.model.Invoice;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InvoiceRepository extends ReactiveCrudRepository<Invoice, Long> {

    Flux<Invoice> findByUserId(Long userId);

    Mono<Invoice> findByIdAndUserId(Long id, Long userId);

    Flux<Invoice> findByStatus(String status);
}
