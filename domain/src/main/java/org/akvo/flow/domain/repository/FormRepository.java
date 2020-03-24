/*
 * Copyright (C) 2018-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.repository;

import org.akvo.flow.domain.entity.DomainForm;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface FormRepository {

    Observable<Boolean> loadForm(String formId, String deviceId);

    Observable<Integer> reloadForms(String deviceId);

    Observable<Integer> downloadForms(String deviceId);

    @NonNull
    Single<Set<String>> loadFormLanguages(@NotNull String formId);

    @NotNull
    Single<DomainForm> parseForm(@NotNull String formId);
}
