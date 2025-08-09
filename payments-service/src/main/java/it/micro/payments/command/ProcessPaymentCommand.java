package it.micro.payments.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {
    @TargetAggregateIdentifier
    private Long invoiceId;

    private Long paymentId;

    private Long userId;

    private Long totalAmount;

    // In a real application, this would include payment details like credit card info
    private String paymentMethod;
}
