package it.micro.tickets.repository;

import it.micro.tickets.model.Reserved;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReservedRepository extends ReactiveCrudRepository<Reserved, Long> {

    Flux<Reserved> findByUserId(Long userId);

    Mono<Reserved> findByTicketIdAndUserId(Long ticketId, Long userId);

    Mono<Void> deleteByTicketIdAndUserId(Long ticketId, Long userId);
}
