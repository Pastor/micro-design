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
@Table("reserved")
public class Reserved {
    @Id
    @Column("ticket_id")
    private Long ticketId;

    @Column("user_id")
    private Long userId;

    @Column("created_at")
    private LocalDateTime createdAt;
}
