package it.micro.saga.simple;

import it.micro.saga.Service;
import it.micro.saga.Transport;
import it.micro.saga.simple.actions.OrderFactory;

import java.util.Map;

import static it.micro.saga.simple.Constants.*;

public final class Main {
    public static void main(String[] args) {
        Transport transport = new Transport.Memory();
        Service market = new Service.Reactive(SERVICE_MARKET, transport, Map.of(ACTION_ORDER, new OrderFactory()));
        Service invoice = new Service.Reactive(SERVICE_INVOICE, transport, Map.of());
        Service payment = new Service.Reactive(SERVICE_PAYMENT, transport, Map.of());
        Service outpost = new Service.Reactive(SERVICE_OUTPOST, transport, Map.of());
        Service warehouse = new Service.Reactive(SERVICE_WAREHOUSE, transport, Map.of());
        market.register(transport, TOPIC_CREATE_ORDER);
        market.register(transport, TOPIC_TRANSACTION_REQUEST_RESULT);
        invoice.register(transport, TOPIC_CREATE_INVOICE);
        invoice.register(transport, TOPIC_TRANSACTION_REQUEST_RESULT);
        payment.register(transport, TOPIC_INVOICE_READY);
        payment.register(transport, TOPIC_TRANSACTION_REQUEST_RESULT);
        warehouse.register(transport, TOPIC_INVOICE_READY);
        warehouse.register(transport, TOPIC_TRANSACTION_REQUEST_RESULT);
        outpost.register(transport, TOPIC_DELIVERY_READY);
        outpost.register(transport, TOPIC_TRANSACTION_REQUEST_RESULT);
        String transactionId = market.execute(ACTION_ORDER,
                Map.of(ORDER_PRODUCT_TYPE, "car", ORDER_PRODUCT, "audi")).orElseThrow();
        System.out.println("ID    : " + transactionId);
        System.out.println("Status: " + market.status(transactionId));
    }
}
