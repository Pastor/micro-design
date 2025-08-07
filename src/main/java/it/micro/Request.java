package it.micro;

import java.util.Map;

import static it.micro.Action.STATUS_KEY;

public record Request(Status status, Information information, Map<String, Object> data, Map<String, Object> result) {
    public Request update(Status status) {
        return new Request(status, information, data, result);
    }

    public Request update(Map<String, Object> params) {
        Status status = Status.valueOf((String) params.get(STATUS_KEY));
        if (this.status != Status.PENDING || this.status != status) {
            throw new IllegalStateException("Invalid status " + this.status);
        }
        return new Request(status, information, data, params);
    }

    public record Information(String serviceName, String sendTopic, String receiveTopic) {

    }
}
