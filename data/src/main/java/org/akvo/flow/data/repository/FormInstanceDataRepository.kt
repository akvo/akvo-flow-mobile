/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.repository

import io.reactivex.Single
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.domain.entity.DomainFormInstance
import org.akvo.flow.domain.repository.FormInstanceRepository
import javax.inject.Inject

class FormInstanceDataRepository @Inject constructor(private val dataSourceFactory: DataSourceFactory) :
    FormInstanceRepository {

    override fun getSavedFormInstance(formId: String, datapointId: String): Single<Long> {
        return dataSourceFactory.dataBaseDataSource.getSavedFormInstance(formId, datapointId)
    }

    override fun getLatestSubmittedFormInstance(
        formId: String,
        datapointId: String,
        maxDate: Long
    ): Single<Long> {
        return dataSourceFactory.dataBaseDataSource.getRecentSubmittedFormInstance(
            formId,
            datapointId,
            maxDate
        )
    }

    override fun createFormInstance(domainFormInstance: DomainFormInstance): Single<Long> {
        return dataSourceFactory.dataBaseDataSource.createFormInstance(domainFormInstance)
    }
}