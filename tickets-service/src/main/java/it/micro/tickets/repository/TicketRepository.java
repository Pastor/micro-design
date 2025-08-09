package it.micro.tickets.repository;

import it.micro.tickets.model.Ticket;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TicketRepository extends ReactiveCrudRepository<Ticket, Long> {

    @Query("SELECT t.* FROM tickets.tickets t " +
           "LEFT JOIN tickets.reserved r ON t.id = r.ticket_id " +
           "LEFT JOIN tickets.associated a ON t.id = a.ticket_id " +
           "WHERE r.ticket_id IS NULL AND a.ticket_id IS NULL")
    Flux<Ticket> findAvailableTickets();
}
