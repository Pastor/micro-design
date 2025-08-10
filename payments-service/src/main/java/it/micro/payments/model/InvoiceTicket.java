package it.micro.payments.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice_tickets")
public class InvoiceTicket {
    @Id
    @Column("ticket_id")
    private Long ticketId;

    @Column("invoice_id")
    private Long invoiceId;

    private Long price;
}
