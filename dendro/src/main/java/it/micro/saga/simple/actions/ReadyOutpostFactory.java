package it.micro.saga.simple.actions;

import it.micro.saga.Action;
import it.micro.saga.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.micro.saga.simple.Constants.SERVICE_PAYMENT;
import static it.micro.saga.simple.Constants.SERVICE_WAREHOUSE;

public final class ReadyOutpostFactory implements Action.Factory {
  @Override
  public Action create(String transactionId, Map<String, Object> params) {
    return new ReadyAction(transactionId, new HashMap<>(params), new AtomicBoolean(false));
  }

  private record ReadyAction(String transactionId, Map<String, Object> params,
                             AtomicBoolean ready) implements Action {

    @Override
    public void handle(String topicName, Map<String, Object> params) {
      String sender = params.get(SENDER_KEY).toString();
      if (SERVICE_PAYMENT.equals(sender)) {
        //NOTICE: Заказ оплачен
        this.params.put(SERVICE_PAYMENT, params);
      } else if (SERVICE_WAREHOUSE.equals(sender)) {
        //Notice: Заказ найден
        this.params.put(SERVICE_WAREHOUSE, params);
      }

      if (this.params.containsKey(SERVICE_WAREHOUSE) && this.params.containsKey(SERVICE_PAYMENT)) {
        ready.set(true);
      }
    }

    @Override
    public boolean isReady() {
      return ready.get();
    }

    @Override
    public List<Request> requests() {
      return List.of();
    }
  }
}
