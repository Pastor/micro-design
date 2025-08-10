package it.micro.payments.controller;

import it.micro.payments.command.CreateInvoiceCommand;
import it.micro.payments.model.Invoice;
import it.micro.payments.model.InvoiceTicket;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private InvoiceRepository invoiceRepository;

    @MockBean
    private InvoiceTicketRepository invoiceTicketRepository;

    @MockBean
    private CommandGateway commandGateway;

    @Test
    void createInvoice_returnsInvoice_andSendsCommand() {
        Invoice saved = new Invoice();
        saved.setId(123L);
        saved.setStatus(Invoice.Status.CREATED.name());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(Mono.just(saved));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        String body = "{\n" +
                "  \"userId\": 42,\n" +
                "  \"ticketItems\": [ { \"ticketId\": 1, \"price\": 1000 }, { \"ticketId\": 2, \"price\": 1000 } ]\n" +
                "}";

        webTestClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(123)
                .jsonPath("$.status").isEqualTo("CREATED");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(commandGateway).send(captor.capture());
        Object sent = captor.getValue();
        assertThat(sent).isInstanceOf(CreateInvoiceCommand.class);
        CreateInvoiceCommand cmd = (CreateInvoiceCommand) sent;
        assertThat(cmd.getInvoiceId()).isEqualTo(123L);
        assertThat(cmd.getUserId()).isEqualTo(42L);
        assertThat(cmd.getTicketItems()).hasSize(2);
    }
}
