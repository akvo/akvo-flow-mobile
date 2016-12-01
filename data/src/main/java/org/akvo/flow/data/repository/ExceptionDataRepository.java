/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.exception.ExceptionFileDataSource;
import org.akvo.flow.domain.repository.ExceptionRepository;

import javax.inject.Inject;

import rx.Observable;

public class ExceptionDataRepository implements ExceptionRepository {

    private final ExceptionFileDataSource exceptionFileDataSource;

    @Inject
    public ExceptionDataRepository(ExceptionFileDataSource exceptionFileDataSource) {
        this.exceptionFileDataSource = exceptionFileDataSource;
    }

    @Override
    public Observable<Boolean> save(Throwable throwable) {
        return exceptionFileDataSource.save(throwable);
    }
}
