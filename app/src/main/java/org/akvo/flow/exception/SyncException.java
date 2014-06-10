package org.akvo.flow.exception;

/**
 * Exception to represent a problem in the Sync process
 */
public class SyncException extends RuntimeException {

    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
