package it.micro.payments.service;

import it.micro.payments.config.KafkaConfig;
import it.micro.payments.event.PaymentProcessedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    @Test
    void sendPaymentEvent_sendsToKafka_andCompletes() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaProducerService service = new KafkaProducerService(kafkaTemplate);

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                10L, 20L, 42L, List.of(1L, 2L), 3000L, "PAYMENT", LocalDateTime.now()
        );

        StepVerifier.create(service.sendPaymentEvent(event))
                .verifyComplete();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.PAYMENT_EVENTS_TOPIC), keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("10");
    }
}
