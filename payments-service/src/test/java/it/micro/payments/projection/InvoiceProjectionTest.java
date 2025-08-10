package it.micro.payments.projection;

import it.micro.payments.event.InvoiceCreatedEvent;
import it.micro.payments.event.PaymentProcessedEvent;
import it.micro.payments.model.Invoice;
import it.micro.payments.model.InvoiceTicket;
import it.micro.payments.model.Payment;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import it.micro.payments.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceProjectionTest {

    @Test
    void onInvoiceCreated_savesInvoiceAndTickets() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceTicketRepository invoiceTicketRepository = mock(InvoiceTicketRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        InvoiceProjection projection = new InvoiceProjection(invoiceRepository, invoiceTicketRepository, paymentRepository);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(invoiceTicketRepository.save(any(InvoiceTicket.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        InvoiceCreatedEvent event = new InvoiceCreatedEvent(
                100L,
                42L,
                List.of(new InvoiceCreatedEvent.TicketItem(1L, 1000L), new InvoiceCreatedEvent.TicketItem(2L, 1000L)),
                Invoice.Status.CREATED.name(),
                LocalDateTime.now()
        );

        projection.on(event);

        ArgumentCaptor<Invoice> invCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, timeout(500)).save(invCaptor.capture());
        assertThat(invCaptor.getValue().getId()).isEqualTo(100L);

        verify(invoiceTicketRepository, timeout(500).times(2)).save(any(InvoiceTicket.class));
    }

    @Test
    void onPaymentProcessed_updatesInvoice_andSavesPayment() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceTicketRepository invoiceTicketRepository = mock(InvoiceTicketRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        InvoiceProjection projection = new InvoiceProjection(invoiceRepository, invoiceTicketRepository, paymentRepository);

        Invoice existing = Invoice.builder().id(100L).status(Invoice.Status.CREATED.name()).build();
        when(invoiceRepository.findById(100L)).thenReturn(Mono.just(existing));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                10L, 100L, 42L, List.of(1L, 2L), 2000L, Invoice.Status.PAYMENT.name(), LocalDateTime.now()
        );

        projection.on(event);

        verify(invoiceRepository, timeout(500)).findById(100L);
        verify(invoiceRepository, timeout(500)).save(any(Invoice.class));
        verify(paymentRepository, timeout(500)).save(any(Payment.class));
    }
}
