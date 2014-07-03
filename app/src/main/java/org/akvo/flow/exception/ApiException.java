package org.akvo.flow.exception;

/**
 * Exception to represent a problem in the FLOW API
 * TODO: Implement error codes for known messages from the API
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
