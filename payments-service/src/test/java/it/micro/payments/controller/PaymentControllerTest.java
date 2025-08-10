package it.micro.payments.controller;

import it.micro.payments.model.Invoice;
import it.micro.payments.model.Payment;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import it.micro.payments.repository.PaymentRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private InvoiceRepository invoiceRepository;

    @MockBean
    private InvoiceTicketRepository invoiceTicketRepository;

    @MockBean
    private CommandGateway commandGateway;

    @Test
    void processPaymentGet_returnsOk_whenInvoiceCreated() {
        Long invoiceId = 1L;
        Long userId = 42L;
        Long total = 3000L;

        Invoice inv = new Invoice();
        inv.setId(invoiceId);
        inv.setStatus(Invoice.Status.CREATED.name());

        when(invoiceRepository.findById(invoiceId)).thenReturn(Mono.just(inv));

        Payment saved = new Payment();
        saved.setId(10L);
        saved.setPrice(total);
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(saved));

        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/payments/process")
                        .queryParam("invoiceId", invoiceId)
                        .queryParam("userId", userId)
                        .queryParam("totalAmount", total)
                        .queryParam("paymentMethod", "CARD")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(invoiceRepository).findById(invoiceId);
        Mockito.verify(paymentRepository).save(any(Payment.class));
        Mockito.verify(commandGateway).send(any());
    }
}
