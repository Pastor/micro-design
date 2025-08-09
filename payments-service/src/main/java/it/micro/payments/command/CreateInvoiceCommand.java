package it.micro.payments.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceCommand {
    @TargetAggregateIdentifier
    private Long invoiceId;

    private Long userId;

    private List<TicketItem> ticketItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItem {
        private Long ticketId;
        private Long price;
    }
}
