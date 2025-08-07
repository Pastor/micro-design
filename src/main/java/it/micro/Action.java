package it.micro;

import java.util.List;
import java.util.Map;

public interface Action {
    String ACTION_KEY = "action";
    String STATUS_KEY = "status";
    String SENDER_KEY = "sender";
    String ERROR_KEY = "error";

    default void preRequests() {

    }

    default void postRequests() {

    }

    default void afterSuccess() {
    }

    default void afterFailure() {
    }

    /**
     * Action готов к исполнению
     * @return
     */
    default boolean isReady() {
        return true;
    }

    List<Request> requests();

    interface Factory {
        Action create(String transactionId, Map<String, Object> params);
    }
}
