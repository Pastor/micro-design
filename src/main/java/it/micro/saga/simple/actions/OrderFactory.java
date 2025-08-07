package it.micro.saga.simple.actions;

import it.micro.saga.Action;
import it.micro.saga.Request;
import it.micro.saga.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.micro.saga.Transport.TRANSACTION_ID;
import static it.micro.saga.simple.Constants.*;

public final class OrderFactory implements Action.Factory {
    @Override
    public Action create(String transactionId, Map<String, Object> params) {
        return new OrderAction(transactionId, params);
    }

    private record OrderAction(String transactionId, Map<String, Object> params) implements Action {
        @Override
        public List<Request> requests() {
            Map<String, Object> params = new HashMap<>();
            params.put(ACTION_KEY, ACTION_CREATE);
            params.put(TRANSACTION_ID, this.transactionId);
            params.put(ORDER_PRODUCT, this.params.get(ORDER_PRODUCT));
            params.put(ORDER_PRODUCT_TYPE, this.params.get(ORDER_PRODUCT_TYPE));
            params.put(ORDER_PRODUCT_ID, 1044587L);
            params.put(ORDER_ADDITIONS, List.of(1004533L, 4459674L));
            return List.of(new Request(
                    Status.PENDING,
                    new Request.Information(
                            SERVICE_INVOICE, TOPIC_CREATE_INVOICE, TOPIC_TRANSACTION_REQUEST_RESULT),
                    Map.copyOf(params),
                    null
            ));
        }
    }
}
