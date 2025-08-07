package it.micro.saga.simple.actions;

import it.micro.saga.Action;
import it.micro.saga.Request;
import it.micro.saga.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.micro.saga.simple.Constants.ACTION_READY;
import static it.micro.saga.simple.Constants.PAYMENT_ID;
import static it.micro.saga.simple.Constants.PAYMENT_METHOD;
import static it.micro.saga.simple.Constants.PAYMENT_RESULT;
import static it.micro.saga.simple.Constants.SERVICE_INVOICE;
import static it.micro.saga.simple.Constants.SERVICE_OUTPOST;
import static it.micro.saga.simple.Constants.TOPIC_CREATE_INVOICE;
import static it.micro.saga.simple.Constants.TOPIC_DELIVERY_READY;
import static it.micro.saga.simple.Constants.TOPIC_TRANSACTION_REQUEST_RESULT;

public final class ReadyPaymentFactory implements Action.Factory {
    @Override
    public Action create(String transactionId, Map<String, Object> params) {
        return new ReadyAction(transactionId, params);
    }

    private record ReadyAction(String transactionId, Map<String, Object> params) implements Action {
        @Override
        public List<Request> requests() {
            Map<String, Object> params = new HashMap<>(params());
            params.put(ACTION_KEY, ACTION_READY);
            params.put(PAYMENT_RESULT, Status.SUCCESS);
            params.put(PAYMENT_METHOD, "offline");
            params.put(PAYMENT_ID, "1299453332");
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
