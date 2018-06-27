/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.data.entity;

public class Transmission {

    private final Long id;
    private final Long respondentId;
    private final String formId;
    private final S3File s3File;

    public Transmission(Long id, Long respondentId, String formId,
            S3File s3File) {
        this.id = id;
        this.respondentId = respondentId;
        this.formId = formId;
        this.s3File = s3File;
    }

    public Long getId() {
        return id;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public String getFormId() {
        return formId;
    }

    public S3File getS3File() {
        return s3File;
    }
}
