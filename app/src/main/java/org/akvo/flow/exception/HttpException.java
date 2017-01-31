/*
 *  Copyright (C) 2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
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
