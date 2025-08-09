package it.micro.payments.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private Long paymentId;
    private Long invoiceId;
    private Long userId;
    private List<Long> ticketIds;
    private Long totalAmount;
    private String status; // "PAYMENT" or "CANCELED"
    private LocalDateTime processedAt;
}
