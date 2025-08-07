package it.micro.saga.simple.actions;

import it.micro.saga.Action;
import it.micro.saga.Request;
import it.micro.saga.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.micro.saga.simple.Constants.ACTION_READY;
import static it.micro.saga.simple.Constants.ORDER_ADDITIONS;
import static it.micro.saga.simple.Constants.SERVICE_OUTPOST;
import static it.micro.saga.simple.Constants.TOPIC_DELIVERY_READY;
import static it.micro.saga.simple.Constants.TOPIC_TRANSACTION_REQUEST_RESULT;
import static it.micro.saga.simple.Constants.WAREHOUSE_ADDITIONS;
import static it.micro.saga.simple.Constants.WAREHOUSE_PRODUCT;

public final class ReadyWarehouseFactory implements Action.Factory {
    @Override
    public Action create(String transactionId, Map<String, Object> params) {
        return new ReadyAction(transactionId, params);
    }

    private record ReadyAction(String transactionId, Map<String, Object> params) implements Action {

        @SuppressWarnings("unchecked")
        @Override
        public List<Request> requests() {
            Map<String, Object> params = new HashMap<>(params());
            params.put(ACTION_KEY, ACTION_READY);
            params.put(WAREHOUSE_PRODUCT, Status.SUCCESS);
            List<Long> additions = (List<Long>) params.get(ORDER_ADDITIONS);
            params.put(WAREHOUSE_ADDITIONS, additions.stream().map(v -> Status.SUCCESS).toList());
            return List.of(new Request(
                    Status.PENDING,
                    new Request.Information(
                            SERVICE_OUTPOST, TOPIC_DELIVERY_READY, TOPIC_TRANSACTION_REQUEST_RESULT),
                    Map.copyOf(params),
                    null
            ));
        }
    }
}
