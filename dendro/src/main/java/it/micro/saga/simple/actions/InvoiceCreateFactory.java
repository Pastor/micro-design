package it.micro.saga.simple.actions;

import it.micro.saga.Action;
import it.micro.saga.Request;
import it.micro.saga.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.micro.saga.Transport.TRANSACTION_ID;
import static it.micro.saga.simple.Constants.ACTION_READY;
import static it.micro.saga.simple.Constants.ORDER_ADDITIONS;
import static it.micro.saga.simple.Constants.ORDER_ADDITIONS_PRICES;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT_ID;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT_PRICE;
import static it.micro.saga.simple.Constants.ORDER_PRODUCT_TYPE;
import static it.micro.saga.simple.Constants.ORDER_SUM;
import static it.micro.saga.simple.Constants.SERVICE_PAYMENT;
import static it.micro.saga.simple.Constants.SERVICE_WAREHOUSE;
import static it.micro.saga.simple.Constants.TOPIC_INVOICE_READY;
import static it.micro.saga.simple.Constants.TOPIC_TRANSACTION_REQUEST_RESULT;

public final class InvoiceCreateFactory implements Action.Factory {
  @Override
  public Action create(String transactionId, Map<String, Object> params) {
    return new CreateAction(transactionId, params);
  }

  private record CreateAction(String transactionId, Map<String, Object> params) implements Action {
    @SuppressWarnings("unchecked")
    @Override
    public List<Request> requests() {
      Map<String, Object> params = new HashMap<>();
      params.put(ACTION_KEY, ACTION_READY);
      params.put(TRANSACTION_ID, this.transactionId);
      params.put(ORDER_PRODUCT, this.params.get(ORDER_PRODUCT));
      params.put(ORDER_PRODUCT_TYPE, this.params.get(ORDER_PRODUCT_TYPE));
      params.put(ORDER_PRODUCT_ID, this.params.get(ORDER_PRODUCT_ID));
      List<Long> numbers = (List<Long>) this.params.get(ORDER_ADDITIONS);
      params.put(ORDER_ADDITIONS, numbers);
      List<Long> prices = numbers.stream().map(v -> 1000L).toList();
      long productPrice = 10000000L;
      params.put(ORDER_ADDITIONS_PRICES, prices);
      params.put(ORDER_PRODUCT_PRICE, productPrice);
      params.put(ORDER_SUM, productPrice + prices.stream().mapToLong(v -> v).sum());
      return List.of(
        new Request(
          Status.PENDING,
          new Request.Information(
            SERVICE_PAYMENT, TOPIC_INVOICE_READY, TOPIC_TRANSACTION_REQUEST_RESULT),
          Map.copyOf(params),
          null
        ),
        new Request(
          Status.PENDING,
          new Request.Information(
            SERVICE_WAREHOUSE, TOPIC_INVOICE_READY, TOPIC_TRANSACTION_REQUEST_RESULT),
          Map.copyOf(params),
          null
        ));
    }
  }
}
