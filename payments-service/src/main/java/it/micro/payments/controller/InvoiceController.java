package it.micro.payments.controller;

import it.micro.payments.command.CreateInvoiceCommand;
import it.micro.payments.model.Invoice;
import it.micro.payments.model.InvoiceTicket;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTicketRepository invoiceTicketRepository;
    private final CommandGateway commandGateway;

    @GetMapping
    public Flux<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Invoice>> getInvoiceById(@PathVariable Long id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tickets")
    public Flux<InvoiceTicket> getInvoiceTickets(@PathVariable Long id) {
        return invoiceTicketRepository.findByInvoiceId(id);
    }

    @PostMapping
    public Mono<ResponseEntity<Invoice>> createInvoice(@RequestBody CreateInvoiceRequest request) {
        log.info("Creating invoice for user: {}", request.getUserId());

        // Generate a new invoice ID
        return Mono.fromCallable(() -> {
            Invoice invoice = new Invoice();
            return invoice;
        }).flatMap(invoice -> {
            return invoiceRepository.save(invoice)
                    .flatMap(savedInvoice -> {
                        // Create the command
                        CreateInvoiceCommand command = new CreateInvoiceCommand(
                                savedInvoice.getId(),
                                request.getUserId(),
                                request.getTicketItems().stream()
                                        .map(item -> new CreateInvoiceCommand.TicketItem(
                                                item.getTicketId(),
                                                item.getPrice()))
                                        .toList()
                        );

                        // Send the command
                        CompletableFuture<Void> future = commandGateway.send(command);
                        return Mono.fromFuture(future)
                                .thenReturn(savedInvoice);
                    });
        }).map(ResponseEntity::ok);
    }

    // Request classes
    public static class CreateInvoiceRequest {
        private Long userId;
        private List<TicketItemRequest> ticketItems;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<TicketItemRequest> getTicketItems() {
            return ticketItems;
        }

        public void setTicketItems(List<TicketItemRequest> ticketItems) {
            this.ticketItems = ticketItems;
        }
    }

    public static class TicketItemRequest {
        private Long ticketId;
        private Long price;

        public Long getTicketId() {
            return ticketId;
        }

        public void setTicketId(Long ticketId) {
            this.ticketId = ticketId;
        }

        public Long getPrice() {
            return price;
        }

        public void setPrice(Long price) {
            this.price = price;
        }
    }
}
