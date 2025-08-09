package it.micro.tickets.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReservedEvent {
    private Long ticketId;
    private Long userId;
    private LocalDateTime reservedAt;
}
