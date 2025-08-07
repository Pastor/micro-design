package it.micro.simple.actions;

import it.micro.Action;

import java.util.Map;

public final class OrderFactory implements Action.Factory {
    @Override
    public Action create(String transactionId, Map<String, Object> params) {
        return null;
    }
}
