package it.micro.tickets.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveTicketCommand {
    @TargetAggregateIdentifier
    private Long ticketId;

    private Long userId;
}
