package it.micro.payments.controller;

import it.micro.payments.command.ProcessPaymentCommand;
import it.micro.payments.model.Invoice;
import it.micro.payments.model.Payment;
import it.micro.payments.repository.InvoiceRepository;
import it.micro.payments.repository.InvoiceTicketRepository;
import it.micro.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceTicketRepository invoiceTicketRepository;
    private final CommandGateway commandGateway;

    @GetMapping
    public Flux<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Payment>> getPaymentById(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/process")
    public Mono<ResponseEntity<Void>> processPayment(@RequestBody ProcessPaymentRequest request) {
        return processPaymentInternal(request);
    }

    @GetMapping("/process")
    public Mono<ResponseEntity<Void>> processPaymentGet(@RequestParam Long invoiceId,
                                                        @RequestParam Long userId,
                                                        @RequestParam Long totalAmount,
                                                        @RequestParam(required = false, defaultValue = "CARD") String paymentMethod) {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setUserId(userId);
        request.setTotalAmount(totalAmount);
        request.setPaymentMethod(paymentMethod);
        return processPaymentInternal(request);
    }

    private Mono<ResponseEntity<Void>> processPaymentInternal(ProcessPaymentRequest request) {
        log.info("Processing payment for invoice: {}", request.getInvoiceId());

        return invoiceRepository.findById(request.getInvoiceId())
                .flatMap(invoice -> {
                    if (!invoice.getStatus().equals(Invoice.Status.CREATED.name())) {
                        return Mono.error(new IllegalStateException("Invoice is not in CREATED state"));
                    }

                    // Create a new payment ID
                    return Mono.fromCallable(() -> {
                        Payment payment = new Payment();
                        payment.setPrice(request.getTotalAmount());
                        return payment;
                    }).flatMap(payment -> paymentRepository.save(payment)
                            .flatMap(savedPayment -> {
                                // Create the command
                                ProcessPaymentCommand command = new ProcessPaymentCommand(
                                        request.getInvoiceId(),
                                        savedPayment.getId(),
                                        request.getUserId(),
                                        request.getTotalAmount(),
                                        request.getPaymentMethod()
                                );

                                // Send the command
                                CompletableFuture<Void> future = commandGateway.send(command);
                                return Mono.fromFuture(future)
                                        .thenReturn(ResponseEntity.ok().<Void>build());
                            }));
                })
                .onErrorResume(e -> {
                    log.error("Error processing payment: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().<Void>build());
                });
    }

    // Request class
    public static class ProcessPaymentRequest {
        private Long invoiceId;
        private Long userId;
        private Long totalAmount;
        private String paymentMethod;

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Long totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }
}
