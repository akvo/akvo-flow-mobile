/*
 *  Copyright (C) 2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.exception;

import java.io.IOException;

/**
 * Exception to represent a problem in the FLOW API
 * TODO: Implement error codes for known messages from the API
 */
public class HttpException extends IOException {

    /**
     * This error codes extend the already existent HTTP status codes, in order to communicate
     * internal API error codes not present in the http layer.
     */
    public interface Status {
        // Custom codes start on 600 (preserving any existent http status unchanged)
        int MALFORMED_RESPONSE = 600;
    }

    private int mStatus;

    public HttpException(String message, int status) {
        super(message);
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
    }

}
