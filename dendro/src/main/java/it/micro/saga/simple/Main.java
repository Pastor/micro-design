package it.micro.saga.simple;

import it.micro.saga.Service;
import it.micro.saga.Transport;
import it.micro.saga.simple.actions.InvoiceCreateFactory;
import it.micro.saga.simple.actions.OrderFactory;
import it.micro.saga.simple.actions.ReadyOutpostFactory;
import it.micro.saga.simple.actions.ReadyPaymentFactory;
import it.micro.saga.simple.actions.ReadyWarehouseFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static it.micro.saga.simple.Constants.ACTION_CREATE;
import static it.micro.saga.simple.Constants.ACTION_ORDER;
import static it.micro.saga.simple.Constants.ACTION_READY;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT_TYPE;
import static it.micro.saga.simple.Constants.SERVICE_INVOICE;
import static it.micro.saga.simple.Constants.SERVICE_MARKET;
import static it.micro.saga.simple.Constants.SERVICE_OUTPOST;
import static it.micro.saga.simple.Constants.SERVICE_PAYMENT;
import static it.micro.saga.simple.Constants.SERVICE_WAREHOUSE;
import static it.micro.saga.simple.Constants.TOPIC_CREATE_INVOICE;
import static it.micro.saga.simple.Constants.TOPIC_CREATE_ORDER;
import static it.micro.saga.simple.Constants.TOPIC_DELIVERY_READY;
import static it.micro.saga.simple.Constants.TOPIC_INVOICE_READY;
import static it.micro.saga.simple.Constants.TOPIC_TRANSACTION_REQUEST_RESULT;

public final class Main {
  public static void main(String[] args) throws InterruptedException {
    ExecutorService executor = Executors.newWorkStealingPool();
    Transport transport = new Transport.Memory(executor);
    Service market = new Service.Reactive(SERVICE_MARKET, transport,
      Map.of(ACTION_ORDER, new OrderFactory()),
      TOPIC_TRANSACTION_REQUEST_RESULT);
    Service invoice = new Service.Reactive(SERVICE_INVOICE, transport,
      Map.of(ACTION_CREATE, new InvoiceCreateFactory()),
      TOPIC_TRANSACTION_REQUEST_RESULT);
    Service payment = new Service.Reactive(SERVICE_PAYMENT, transport,
      Map.of(ACTION_READY, new ReadyPaymentFactory()),
      TOPIC_TRANSACTION_REQUEST_RESULT);
    Service warehouse = new Service.Reactive(SERVICE_WAREHOUSE, transport,
      Map.of(ACTION_READY, new ReadyWarehouseFactory()),
      TOPIC_TRANSACTION_REQUEST_RESULT);
    Service outpost = new Service.Reactive(SERVICE_OUTPOST, transport,
      Map.of(ACTION_READY, new ReadyOutpostFactory()),
      TOPIC_TRANSACTION_REQUEST_RESULT);
    market.register(transport, TOPIC_CREATE_ORDER);
    invoice.register(transport, TOPIC_CREATE_INVOICE);
    payment.register(transport, TOPIC_INVOICE_READY);
    warehouse.register(transport, TOPIC_INVOICE_READY);
    outpost.register(transport, TOPIC_DELIVERY_READY);
    String transactionId = market.execute(ACTION_ORDER,
      Map.of(ORDER_PRODUCT_TYPE, "car", ORDER_PRODUCT, "audi")).orElseThrow();
    Thread.sleep(10000);
    System.out.println("ID    : " + transactionId);
    System.out.println("Status: " + market.status(transactionId));
    System.out.println("Result: " + market.result(transactionId));
  }
}
