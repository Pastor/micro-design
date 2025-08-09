package it.micro.payments.service;

import it.micro.payments.config.KafkaConfig;
import it.micro.payments.event.PaymentProcessedEvent;
import it.micro.payments.event.kafka.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<Void> sendPaymentEvent(PaymentProcessedEvent event) {
        log.info("Sending payment event to Kafka for payment ID: {}", event.getPaymentId());

        PaymentEvent.PaymentStatus status = event.getStatus().equals("PAYMENT")
                ? PaymentEvent.PaymentStatus.COMPLETED
                : PaymentEvent.PaymentStatus.FAILED;

        PaymentEvent paymentEvent = new PaymentEvent(
                event.getPaymentId(),
                event.getInvoiceId(),
                event.getTicketIds(),
                event.getUserId(),
                status,
                LocalDateTime.now()
        );

        return Mono.fromCallable(() -> {
            kafkaTemplate.send(KafkaConfig.PAYMENT_EVENTS_TOPIC,
                    event.getPaymentId().toString(), paymentEvent);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
