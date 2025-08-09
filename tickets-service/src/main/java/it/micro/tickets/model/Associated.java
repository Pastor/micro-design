package it.micro.tickets.model;

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
@Table("associated")
public class Associated {
    @Id
    @Column("ticket_id")
    private Long ticketId;

    @Column("user_id")
    private Long userId;

    @Column("reason_type")
    private String reasonType;

    @Column("reason_reference_id")
    private Long reasonReferenceId;

    @Column("created_at")
    private LocalDateTime createdAt;
}
