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
package org.akvo.flow.data.repository

import io.reactivex.Observable
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.entity.ApiFormHeader
import org.akvo.flow.data.entity.form.DataForm
import org.akvo.flow.data.entity.form.DataFormMapper
import org.akvo.flow.data.entity.form.DomainFormMapper
import org.akvo.flow.data.entity.form.FormHeaderParser
import org.akvo.flow.data.entity.form.FormIdMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.FlowFileBrowser
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.repository.FormRepository
import org.akvo.flow.utils.XmlFormParser
import org.akvo.flow.utils.entity.Form
import timber.log.Timber
import javax.inject.Inject


class FormDataRepository @Inject constructor(
    private val formHeaderParser: FormHeaderParser,
    private val xmlParser: XmlFormParser,
    private val restApi: RestApi,
    private val dataSourceFactory: DataSourceFactory,
    private val formIdMapper: FormIdMapper,
    private val s3RestApi: S3RestApi,
    private val domainFormMapper: DomainFormMapper,
    private val dataFormMapper: DataFormMapper
) : FormRepository {

    override fun loadForm(formId: String?, deviceId: String?): Observable<Boolean?>? {
        val dataBaseDataSource = dataSourceFactory.dataBaseDataSource
        return if (TEST_FORM_ID == formId) {
            dataBaseDataSource.installTestForm()
        } else {
            downloadFormHeader(formId, deviceId)
        }
    }

    override fun reloadForms(deviceId: String?): Observable<Int?>? {
        val dataBaseDataSource = dataSourceFactory.dataBaseDataSource
        return dataBaseDataSource.formIds
            .map { cursor -> formIdMapper.mapToFormId(cursor) }
            .concatMap { formIds ->
                dataBaseDataSource.deleteAllForms()
                    .concatMap { downloadFormHeaders(formIds, deviceId) }
            }
    }

    override fun downloadForms(deviceId: String?): Observable<Int?>? {
        return restApi.downloadFormsHeader(deviceId)
            .map { response -> formHeaderParser.parseMultiple(response) }
            .concatMap { apiFormHeaders -> downloadForms(apiFormHeaders) }
    }

    override suspend fun loadFormLanguages(formId: String): Set<String> {
        return xmlParser.parseLanguages(dataSourceFactory.fileDataSource.getFormFile(formId))
    }

    override fun getForm(formId: String): DomainForm {
        return domainFormMapper.mapForm(dataSourceFactory.dataBaseDataSource.getForm(formId))
    }

    override fun getForms(surveyId: Long): List<DomainForm> {
        return domainFormMapper.mapForms(dataSourceFactory.dataBaseDataSource.getForms(surveyId))
    }

    override suspend fun getFormWithGroups(formId: String): DomainForm {
        //TODO: once questions are in database we need to parse from db everything without opening xml form
        return domainFormMapper.mapForm(
            dataSourceFactory.dataBaseDataSource.getFormWithGroups(formId),
            parseForm(formId)
        )
    }

    private fun parseForm(formId: String): Form {
        return xmlParser.parseXmlForm(dataSourceFactory.fileDataSource.getFormFile(formId))
    }

    private fun downloadFormHeader(formId: String?, deviceId: String?): Observable<Boolean?> {
        return restApi.downloadFormHeader(formId!!, deviceId)
            .map { response -> formHeaderParser.parseOne(response) }
            .concatMap { apiFormHeader -> insertAndDownload(apiFormHeader) }
    }

    private fun downloadForms(apiFormHeaders: List<ApiFormHeader>): Observable<Int> {
        return Observable.fromIterable(apiFormHeaders)
            .concatMap { apiFormHeader -> insertAndDownload(apiFormHeader) }
            .toList()
            .toObservable()
            .map { booleans -> booleans.size }
    }

    private fun insertAndDownload(apiFormHeader: ApiFormHeader): Observable<Boolean> {
        return dataSourceFactory.dataBaseDataSource.insertSurveyGroup(apiFormHeader)
            .concatMap { downloadForm(apiFormHeader) }
    }

    private fun downloadFormHeaders(formIds: List<String>, deviceId: String?): Observable<Int> {
        return Observable.fromIterable(formIds)
            .concatMap<Boolean> { formId -> downloadFormHeader(formId, deviceId) }
            .toList()
            .toObservable()
            .map { booleans -> booleans.size }
    }

    private fun downloadForm(apiFormHeader: ApiFormHeader): Observable<Boolean> {
        return dataSourceFactory.dataBaseDataSource.formNeedsUpdate(apiFormHeader)
            .concatMap { updateNeeded ->
                if (updateNeeded) {
                    downloadAndSaveForm(apiFormHeader)
                } else {
                    Observable.just(true)
                }
            }
    }

    private fun downloadAndSaveForm(apiFormHeader: ApiFormHeader): Observable<Boolean> {
        return downloadAndExtractFile(
            apiFormHeader.id + FlowFileBrowser.ZIP_SUFFIX,
            FlowFileBrowser.DIR_FORMS
        )
            .concatMap { saveForm(apiFormHeader) }
    }

    private fun downloadAndExtractFile(
        fileName: String,
        folder: String,
    ): Observable<Boolean> {
        return s3RestApi.downloadArchive(fileName)
            .concatMap { responseBody ->
                dataSourceFactory.fileDataSource.extractRemoteArchive(responseBody, folder)
            }
    }

    private fun saveForm(apiFormHeader: ApiFormHeader): Observable<Boolean> {
        val form = dataFormMapper.mapForm(xmlParser.parseXmlForm(dataSourceFactory.fileDataSource.getFormFile(apiFormHeader.id), apiFormHeader.version.toDouble()))
        return downloadResources(form)
            .concatMap {
                saveFormAndGroups(form, true)
            }
            .doOnError { throwable ->
                Timber.e(throwable)
                saveFormAndGroups(form, false)
            }
    }

    private fun saveFormAndGroups(form: DataForm, resourcesDownloaded: Boolean): Observable<Boolean> {
        val dataBaseDataSource = dataSourceFactory.dataBaseDataSource
        dataBaseDataSource.saveForm(resourcesDownloaded, form)
        dataBaseDataSource.saveGroups(form)
        return Observable.just(true)
    }

    private fun downloadResources(form: DataForm): Observable<Boolean> {
        return Observable.fromIterable(form.getResources())
            .concatMap { resource ->
                downloadAndExtractFile(
                    resource + FlowFileBrowser.ZIP_SUFFIX,
                    FlowFileBrowser.DIR_RES
                )
            }
            .toList()
            .toObservable()
            .map { true }
    }

    companion object {
        private const val TEST_FORM_ID = "0"
    }

}