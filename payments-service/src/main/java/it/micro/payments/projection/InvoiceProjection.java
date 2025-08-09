package it.micro.payments.projection;

import it.micro.payments.event.InvoiceCreatedEvent;
import it.micro.payments.event.PaymentProcessedEvent;
import it.micro.payments.model.Invoice;
import it.micro.payments.model.InvoiceTicket;
import it.micro.payments.model.Payment;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import it.micro.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceProjection {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTicketRepository invoiceTicketRepository;
    private final PaymentRepository paymentRepository;

    @EventHandler
    public void on(InvoiceCreatedEvent event) {
        log.info("Handling InvoiceCreatedEvent: {}", event);

        // Create and save the invoice
        Invoice invoice = Invoice.builder()
                .id(event.getInvoiceId())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .build();

        Mono<Invoice> savedInvoiceMono = invoiceRepository.save(invoice);

        // Create and save the invoice tickets
        Flux<InvoiceTicket> invoiceTicketFlux = Flux.fromIterable(event.getTicketItems())
                .map(item -> InvoiceTicket.builder()
                        .invoiceId(event.getInvoiceId())
                        .ticketId(item.getTicketId())
                        .price(item.getPrice())
                        .build())
                .flatMap(invoiceTicketRepository::save);

        // Wait for all operations to complete
        savedInvoiceMono
                .thenMany(invoiceTicketFlux)
                .doOnComplete(() -> log.info("Invoice {} created with {} tickets",
                        event.getInvoiceId(), event.getTicketItems().size()))
                .subscribe();
    }

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("Handling PaymentProcessedEvent: {}", event);

        // Update the invoice status
        invoiceRepository.findById(event.getInvoiceId())
                .flatMap(invoice -> {
                    invoice.setStatus(event.getStatus());
                    invoice.setCompletedAt(event.getProcessedAt());
                    return invoiceRepository.save(invoice);
                })
                .subscribe(invoice -> log.info("Updated invoice {} status to {}",
                        invoice.getId(), invoice.getStatus()));

        // Create and save the payment
        Payment payment = Payment.builder()
                .id(event.getPaymentId())
                .price(event.getTotalAmount())
                .status(event.getStatus())
                .createdAt(LocalDateTime.now())
                .completedAt(event.getProcessedAt())
                .build();

        paymentRepository.save(payment)
                .subscribe(savedPayment -> log.info("Saved payment {}", savedPayment.getId()));
    }
}
