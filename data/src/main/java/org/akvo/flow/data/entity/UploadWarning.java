/*
 * Copyright (C) 2023 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.data.entity;

public class UploadWarning extends UploadSuccess {
    private final int formId;
    private final String warning;
    private final String message;

    public UploadWarning(long surveyInstanceId, int formId, String warning, String message) {
        super(surveyInstanceId);
        this.formId = formId;
        this.warning = warning;
        this.message = message;
    }

    public int getFormId() {
        return this.formId;
    }

    public String getWarning() {
        return this.warning;
    }

    public String getMessage() {
        return this.message;
    }
}
