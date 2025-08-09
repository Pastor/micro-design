package it.micro.tickets.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssociatedEvent {
    private Long ticketId;
    private Long userId;
    private String reasonType;
    private Long reasonReferenceId;
    private LocalDateTime associatedAt;
}
