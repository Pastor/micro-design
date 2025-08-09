package it.micro.tickets.listener;

import it.micro.tickets.command.AssociateTicketCommand;
import it.micro.tickets.command.ReleaseReservationCommand;
import it.micro.tickets.config.KafkaConfig;
import it.micro.tickets.event.kafka.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final CommandGateway commandGateway;

    @KafkaListener(topics = KafkaConfig.PAYMENT_EVENTS_TOPIC, groupId = "tickets-service")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {}", event);

        if (event.getStatus() == PaymentEvent.PaymentStatus.COMPLETED) {
            // Payment successful, associate tickets with user
            Flux.fromIterable(event.getTicketIds())
                    .flatMap(ticketId -> {
                        AssociateTicketCommand command = new AssociateTicketCommand(
                                ticketId,
                                event.getUserId(),
                                "INVOICING",
                                event.getInvoiceId()
                        );
                        return Mono.fromFuture(commandGateway.send(command))
                                .subscribeOn(Schedulers.boundedElastic());
                    })
                    .doOnError(e -> log.error("Error associating tickets: {}", e.getMessage()))
                    .subscribe();
        } else {
            // Payment failed, release ticket reservations
            Flux.fromIterable(event.getTicketIds())
                    .flatMap(ticketId -> {
                        ReleaseReservationCommand command = new ReleaseReservationCommand(
                                ticketId,
                                event.getUserId()
                        );
                        return Mono.fromFuture(commandGateway.send(command))
                                .subscribeOn(Schedulers.boundedElastic());
                    })
                    .doOnError(e -> log.error("Error releasing ticket reservations: {}", e.getMessage()))
                    .subscribe();
        }
    }
}
