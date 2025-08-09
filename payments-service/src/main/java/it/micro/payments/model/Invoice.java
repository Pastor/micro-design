package it.micro.payments.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("invoices")
public class Invoice {
    @Id
    private Long id;

    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("sent_payment_at")
    private LocalDateTime sentPaymentAt;

    @Column("completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        CREATED,
        SENT_PAYMENT,
        PAYMENT,
        CANCELED
    }
}
