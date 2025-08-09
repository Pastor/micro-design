package it.micro.payments.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreatedEvent {
    private Long invoiceId;
    private Long userId;
    private List<TicketItem> ticketItems;
    private String status;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItem {
        private Long ticketId;
        private Long price;
    }
}
