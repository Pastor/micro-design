package it.micro.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String TICKET_EVENTS_TOPIC = "ticket-events";

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketEventsTopic() {
        return TopicBuilder.name(TICKET_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
