package org.yaroslaavl.cvservice.exception;

public class OutOfQuantityException extends RuntimeException {
    public OutOfQuantityException(String message) {
        super(message);
    }
}
