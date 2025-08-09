package it.micro.tickets.aggregate;

import it.micro.tickets.command.AssociateTicketCommand;
import it.micro.tickets.command.CreateTicketCommand;
import it.micro.tickets.command.ReleaseReservationCommand;
import it.micro.tickets.command.ReserveTicketCommand;
import it.micro.tickets.event.ReservationReleasedEvent;
import it.micro.tickets.event.TicketAssociatedEvent;
import it.micro.tickets.event.TicketCreatedEvent;
import it.micro.tickets.event.TicketReservedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
public class TicketAggregate {

    @AggregateIdentifier
    private Long id;
    private boolean reserved;
    private boolean associated;
    private Long reservedByUserId;

    @CommandHandler
    public TicketAggregate(CreateTicketCommand command) {
        AggregateLifecycle.apply(new TicketCreatedEvent(command.getTicketId()));
    }

    @EventSourcingHandler
    public void on(TicketCreatedEvent event) {
        this.id = event.getTicketId();
        this.reserved = false;
        this.associated = false;
    }

    @CommandHandler
    public void handle(ReserveTicketCommand command) {
        if (reserved || associated) {
            throw new IllegalStateException("Ticket is already reserved or associated");
        }

        AggregateLifecycle.apply(new TicketReservedEvent(
                command.getTicketId(),
                command.getUserId(),
                LocalDateTime.now()
        ));
    }

    @EventSourcingHandler
    public void on(TicketReservedEvent event) {
        this.reserved = true;
        this.reservedByUserId = event.getUserId();
    }

    @CommandHandler
    public void handle(AssociateTicketCommand command) {
        if (associated) {
            throw new IllegalStateException("Ticket is already associated");
        }

        if (!reserved || !reservedByUserId.equals(command.getUserId())) {
            throw new IllegalStateException("Ticket must be reserved by the same user before association");
        }

        AggregateLifecycle.apply(new TicketAssociatedEvent(
                command.getTicketId(),
                command.getUserId(),
                command.getReasonType(),
                command.getReasonReferenceId(),
                LocalDateTime.now()
        ));
    }

    @EventSourcingHandler
    public void on(TicketAssociatedEvent event) {
        this.reserved = false;
        this.associated = true;
    }

    @CommandHandler
    public void handle(ReleaseReservationCommand command) {
        if (!reserved || !reservedByUserId.equals(command.getUserId())) {
            throw new IllegalStateException("Ticket is not reserved by this user");
        }

        if (associated) {
            throw new IllegalStateException("Ticket is already associated and cannot be released");
        }

        AggregateLifecycle.apply(new ReservationReleasedEvent(
                command.getTicketId(),
                command.getUserId(),
                LocalDateTime.now()
        ));
    }

    @EventSourcingHandler
    public void on(ReservationReleasedEvent event) {
        this.reserved = false;
        this.reservedByUserId = null;
    }
}
