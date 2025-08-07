package it.micro.simple;

import it.micro.Service;
import it.micro.Transport;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {
    public static void main(String[] args) {
        Transport transport = new Transport.Memory();
        ExecutorService executor = Executors.newWorkStealingPool();
        Service market = new Service.Reactive("market", transport, Map.of());
        Service invoice = new Service.Reactive("invoice", transport, Map.of());
        Service payment = new Service.Reactive("payment", transport, Map.of());
        Service outpost = new Service.Reactive("outpost", transport, Map.of());
        Service warehouse = new Service.Reactive("warehouse", transport, Map.of());
        market.register(transport, Topics.CREATE_ORDER);
        invoice.register(transport, Topics.CREATE_INVOICE);
        payment.register(transport, Topics.INVOICE_READY);
        warehouse.register(transport, Topics.INVOICE_READY);
        outpost.register(transport, Topics.DELIVERY_READY);
        market.execute("order", Map.of("car", "audi"));
    }
}
