package it.micro.payments.event.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private Long paymentId;
    private Long invoiceId;
    private List<Long> ticketIds;
    private Long userId;
    private PaymentStatus status;
    private LocalDateTime timestamp;

    public enum PaymentStatus {
        COMPLETED,
        FAILED
    }
}
