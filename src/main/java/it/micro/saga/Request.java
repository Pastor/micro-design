package it.micro.saga;

import java.util.HashMap;
import java.util.Map;

import static it.micro.saga.Action.SENDER_KEY;
import static it.micro.saga.Action.STATUS_KEY;

public record Request(Status status, Information information, Map<String, Object> data, Map<String, Object> result) {
    public Request update(Status status) {
        return new Request(status, information, data, result);
    }

    public Request update(Map<String, Object> params) {
        Object object = params.get(STATUS_KEY);
        Status status;
        if (object instanceof String text) {
            status = Status.valueOf(text);
        } else if (object instanceof Status s) {
            status = s;
        } else {
            throw new IllegalArgumentException(String.format("invalid status object %s", object));
        }
        if (this.status != Status.PENDING && this.status != status) {
            throw new IllegalStateException("Invalid status " + this.status);
        }
        return new Request(status, information, data, params);
    }

    public Request update(Status status, String sender) {
        Map<String, Object> map = new HashMap<>(data);
        map.put(SENDER_KEY, sender);
        return new Request(status, information, Map.copyOf(map), result);
    }

    public record Information(String serviceName, String sendTopic, String receiveTopic) {

    }

    @Override
    public String toString() {
        return information.serviceName + ":" + status.toString();
    }
}
