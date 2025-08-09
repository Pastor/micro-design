package it.micro.tickets.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for creating invoices in the payments service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    @Value("${payments.service.url:http://payments-service:8081}")
    private String paymentsServiceUrl;

    private final WebClient.Builder webClientBuilder;

    /**
     * Creates an invoice for the given tickets
     *
     * @param userId    the user ID
     * @param ticketIds the list of ticket IDs
     * @return a Mono containing the invoice ID
     */
    public Mono<Long> createInvoice(Long userId, List<Long> ticketIds) {
        log.info("Creating invoice for user {} with {} tickets", userId, ticketIds.size());

        // For simplicity, we'll use a fixed price for each ticket
        long ticketPrice = 1000L; // $10.00 in cents

        List<TicketItemRequest> ticketItems = new ArrayList<>();
        for (Long ticketId : ticketIds) {
            ticketItems.add(new TicketItemRequest(ticketId, ticketPrice));
        }

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setUserId(userId);
        request.setTicketItems(ticketItems);

        return webClientBuilder.build()
                .post()
                .uri(paymentsServiceUrl + "/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InvoiceResponse.class)
                .map(response -> response.getId())
                .doOnSuccess(invoiceId -> log.info("Successfully created invoice {} for user {}", invoiceId, userId))
                .doOnError(e -> log.error("Error creating invoice for user {}: {}", userId, e.getMessage()));
    }

    /**
     * Gets the payment URL for the given invoice ID
     *
     * @param invoiceId the invoice ID
     * @return the payment URL
     */
    public String getPaymentUrl(Long invoiceId) {
        return paymentsServiceUrl + "/api/payments/process?invoiceId=" + invoiceId;
    }

    // Request and response classes matching the payments service API
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

        public TicketItemRequest(Long ticketId, Long price) {
            this.ticketId = ticketId;
            this.price = price;
        }

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

    public static class InvoiceResponse {
        private Long id;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
