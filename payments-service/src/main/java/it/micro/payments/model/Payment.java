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
@Table("payments")
public class Payment {
    @Id
    private Long id;

    private Long price;

    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        CREATED,
        PAYMENT,
        CANCELED
    }
}
