package it.micro.payments.aggregate;

import it.micro.payments.command.CreateInvoiceCommand;
import it.micro.payments.command.ProcessPaymentCommand;
import it.micro.payments.event.InvoiceCreatedEvent;
import it.micro.payments.event.PaymentProcessedEvent;
import it.micro.payments.model.Invoice;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Aggregate
@NoArgsConstructor
public class InvoiceAggregate {

    @AggregateIdentifier
    private Long id;
    private Long userId;
    private List<TicketItem> ticketItems;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentPaymentAt;
    private LocalDateTime completedAt;

    @CommandHandler
    public InvoiceAggregate(CreateInvoiceCommand command) {
        AggregateLifecycle.apply(new InvoiceCreatedEvent(
                command.getInvoiceId(),
                command.getUserId(),
                command.getTicketItems().stream()
                        .map(item -> new InvoiceCreatedEvent.TicketItem(item.getTicketId(), item.getPrice()))
                        .collect(Collectors.toList()),
                Invoice.Status.CREATED.name(),
                LocalDateTime.now()
        ));
    }

    @EventSourcingHandler
    public void on(InvoiceCreatedEvent event) {
        this.id = event.getInvoiceId();
        this.userId = event.getUserId();
        this.ticketItems = event.getTicketItems().stream()
                .map(item -> new TicketItem(item.getTicketId(), item.getPrice()))
                .collect(Collectors.toList());
        this.status = event.getStatus();
        this.createdAt = event.getCreatedAt();
    }

    @CommandHandler
    public void handle(ProcessPaymentCommand command) {
        if (!status.equals(Invoice.Status.CREATED.name())) {
            throw new IllegalStateException("Invoice is not in CREATED state");
        }

        // In a real application, this would include payment processing logic
        // For simplicity, we'll assume the payment is successful if the amount matches

        long calculatedTotal = ticketItems.stream()
                .mapToLong(TicketItem::getPrice)
                .sum();

        boolean paymentSuccessful = command.getTotalAmount() >= calculatedTotal;
        String newStatus = paymentSuccessful ?
                Invoice.Status.PAYMENT.name() :
                Invoice.Status.CANCELED.name();

        List<Long> ticketIds = ticketItems.stream()
                .map(TicketItem::getTicketId)
                .collect(Collectors.toList());

        AggregateLifecycle.apply(new PaymentProcessedEvent(
                command.getPaymentId(),
                command.getInvoiceId(),
                command.getUserId(),
                ticketIds,
                command.getTotalAmount(),
                newStatus,
                LocalDateTime.now()
        ));
    }

    @EventSourcingHandler
    public void on(PaymentProcessedEvent event) {
        this.status = event.getStatus();
        this.completedAt = event.getProcessedAt();
    }

    @NoArgsConstructor
    private static class TicketItem {
        private Long ticketId;
        private Long price;

        public TicketItem(Long ticketId, Long price) {
            this.ticketId = ticketId;
            this.price = price;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public Long getPrice() {
            return price;
        }
    }
}
