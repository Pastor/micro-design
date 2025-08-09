package it.micro.payments.repository;

import it.micro.payments.model.InvoiceTicket;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface InvoiceTicketRepository extends ReactiveCrudRepository<InvoiceTicket, Long> {

    Flux<InvoiceTicket> findByInvoiceId(Long invoiceId);

    Flux<InvoiceTicket> findByTicketId(Long ticketId);
}
