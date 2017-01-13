/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

/**
 * Exception to be thrown if data provided by the user is invalid
 * 
 * @author Christopher Fagiani
 */
public class ValidationException extends Exception {
    public static final String TOO_SMALL = "too small";
    public static final String TOO_LARGE = "too large";
    public static final String INVALID_DATATYPE = "bad datatype";

    private static final long serialVersionUID = 677744340304381823L;

    private String type;

    public ValidationException(String message, String type, Exception e) {
        super(message, e);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
