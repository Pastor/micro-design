package it.micro.tickets.repository;

import it.micro.tickets.model.Associated;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AssociatedRepository extends ReactiveCrudRepository<Associated, Long> {

    Flux<Associated> findByUserId(Long userId);

    Flux<Associated> findByReasonTypeAndReasonReferenceId(String reasonType, Long reasonReferenceId);

    Mono<Associated> findByTicketId(Long ticketId);
}
