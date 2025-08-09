package it.micro.tickets.controller;

import it.micro.tickets.command.CreateTicketCommand;
import it.micro.tickets.command.ReserveTicketCommand;
import it.micro.tickets.model.Ticket;
import it.micro.tickets.repository.TicketRepository;
import it.micro.tickets.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketRepository ticketRepository;
    private final CommandGateway commandGateway;
    private final InvoiceService invoiceService;

    @GetMapping
    public Flux<Ticket> getAvailableTickets() {
        return ticketRepository.findAvailableTickets();
    }

    @PostMapping
    public Mono<ResponseEntity<Ticket>> createTicket() {
        return Mono.fromCallable(() -> {
            Ticket ticket = new Ticket();
            return ticket;
        }).flatMap(ticket -> {
            return ticketRepository.save(ticket)
                    .flatMap(savedTicket -> {
                        CompletableFuture<Void> future = commandGateway.send(
                                new CreateTicketCommand(savedTicket.getId())
                        );
                        return Mono.fromFuture(future)
                                .thenReturn(savedTicket);
                    });
        }).map(ResponseEntity::ok);
    }

    @PostMapping("/reserve")
    public Mono<ResponseEntity<Object>> reserveTickets(
            @RequestParam Long userId,
            @RequestBody List<Long> ticketIds) {

        log.info("Reserving {} tickets for user {}", ticketIds.size(), userId);

        // Step 1: Reserve all tickets
        return Flux.fromIterable(ticketIds)
                .flatMap(ticketId -> {
                    CompletableFuture<Void> future = commandGateway.send(
                            new ReserveTicketCommand(ticketId, userId)
                    );
                    return Mono.fromFuture(future);
                })
                // Step 2: Create an invoice for the reserved tickets
                .then(invoiceService.createInvoice(userId, ticketIds))
                // Step 3: Redirect to payment page
                .map(invoiceId -> {
                    String paymentUrl = invoiceService.getPaymentUrl(invoiceId);
                    log.info("Redirecting to payment page: {}", paymentUrl);
                    return ResponseEntity.status(302)
                            .location(URI.create(paymentUrl))
                            .build();
                })
                .doOnError(e -> log.error("Error during ticket reservation: {}", e.getMessage()));
    }
}
