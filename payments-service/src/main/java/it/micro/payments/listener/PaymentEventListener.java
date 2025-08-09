package it.micro.payments.listener;

import it.micro.payments.event.PaymentProcessedEvent;
import it.micro.payments.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final KafkaProducerService kafkaProducerService;

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent: {}", event);

        // Send the payment event to Kafka
        Mono<Void> result = kafkaProducerService.sendPaymentEvent(event);

        // Subscribe to trigger the reactive chain
        result.subscribe(
            null,
            error -> log.error("Error sending payment event to Kafka: {}", error.getMessage()),
            () -> log.info("Successfully sent payment event to Kafka for payment ID: {}", event.getPaymentId())
        );
    }
}
