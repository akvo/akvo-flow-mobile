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
package org.akvo.flow.domain.repository

import io.reactivex.Observable
import org.akvo.flow.domain.entity.DomainForm
import java.io.File


interface FormRepository {
    fun loadForm(formId: String?, deviceId: String?): Observable<Boolean?>?
    fun reloadForms(deviceId: String?): Observable<Int?>?
    fun downloadForms(deviceId: String?): Observable<Int?>?
    fun getForm(formId: String): DomainForm
    fun getForms(surveyId: Long): List<DomainForm>
    suspend fun loadFormLanguages(formId: String): Set<String>
    suspend fun getFormWithGroups(formId: String): DomainForm
    suspend fun processZipFile(file: File, instanceUrl: String, awsBucket: String)
}
