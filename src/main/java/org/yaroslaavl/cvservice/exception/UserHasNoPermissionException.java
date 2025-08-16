package org.yaroslaavl.cvservice.exception;

public class UserHasNoPermissionException extends RuntimeException {
    public UserHasNoPermissionException(String message) {
        super(message);
    }
}
