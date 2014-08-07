package org.akvo.flow.exception;

import org.apache.http.HttpStatus;

/**
 * Exception to represent a problem in the FLOW API
 * TODO: Implement error codes for known messages from the API
 */
public class ApiException extends RuntimeException {

    /**
     * This error codes extend the already existent HTTP status codes, in order to communicate
     * internal API error codes not present in the http layer.
     */
    public interface Status extends HttpStatus {
        // Custom codes start on 600 (preserving any existent http status unchanged)
        int MALFORMED_RESPONSE = 600;
    }

    private int mStatus;

    public ApiException(String message, int status) {
        super(message);
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
    }

}
