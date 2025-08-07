package it.micro.saga;

public enum Status {
    NOT_READY,
    READY,
    PENDING,
    SUCCESS,
    FAILURE,
    COMPENSATION,
    COMPENSATION_COMPLETE,
    NOT_FOUND
}
