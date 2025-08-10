package it.micro.tickets.controller;

import it.micro.tickets.service.InvoiceService;
import it.micro.tickets.repository.TicketRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private CommandGateway commandGateway;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    void reserveTickets_redirectsToPayment_withExpectedLocation() {
        Long userId = 42L;
        List<Long> ticketIds = List.of(1L, 2L);
        Long invoiceId = 77L;
        Long totalAmount = ticketIds.size() * 1000L;
        String expectedUrl = "http://payments-service:8081/api/payments/process?invoiceId=" + invoiceId +
                "&userId=" + userId +
                "&totalAmount=" + totalAmount +
                "&paymentMethod=CARD";

        // For each reserve command
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Create invoice returns invoiceId
        when(invoiceService.createInvoice(eq(userId), eq(ticketIds))).thenReturn(Mono.just(invoiceId));
        // URL generation
        when(invoiceService.getPaymentUrl(invoiceId, userId, totalAmount)).thenReturn(expectedUrl);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/tickets/reserve")
                        .queryParam("userId", userId).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ticketIds)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", expectedUrl);

        Mockito.verify(commandGateway, Mockito.times(ticketIds.size())).send(any());
        Mockito.verify(invoiceService).createInvoice(userId, ticketIds);
        Mockito.verify(invoiceService).getPaymentUrl(invoiceId, userId, totalAmount);
    }
}
