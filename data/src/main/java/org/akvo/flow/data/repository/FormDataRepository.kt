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
import org.akvo.flow.data.entity.form.DataSurveyMapper
import org.akvo.flow.data.entity.form.DomainFormMapper
import org.akvo.flow.data.entity.form.FormHeaderParser
import org.akvo.flow.data.entity.form.FormIdMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.FlowFileBrowser.DIR_FORMS
import org.akvo.flow.data.util.FlowFileBrowser.DIR_RES
import org.akvo.flow.data.util.FlowFileBrowser.ZIP_SUFFIX
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.exception.CascadeProcessingError
import org.akvo.flow.domain.exception.WrongDashboardError
import org.akvo.flow.domain.interactor.bootstrap.BootstrapProcessor
import org.akvo.flow.domain.repository.FormRepository
import org.akvo.flow.utils.XmlFormParser
import org.akvo.flow.utils.entity.Question
import timber.log.Timber
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject


class FormDataRepository @Inject constructor(
    private val formHeaderParser: FormHeaderParser,
    private val xmlParser: XmlFormParser,
    private val restApi: RestApi,
    private val dataSourceFactory: DataSourceFactory,
    private val formIdMapper: FormIdMapper,
    private val s3RestApi: S3RestApi,
    private val domainFormMapper: DomainFormMapper,
    private val dataFormMapper: DataFormMapper,
    private val dataSurveyMapper: DataSurveyMapper
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
            parseFormQuestions(formId)
        )
    }

    override suspend fun processZipFile(file: File, instanceUrl: String, awsBucket: String) {
        val zipFile = ZipFile(file)
        val entries: Enumeration<out ZipEntry> = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name ?: ""
            when {
                entryName.endsWith(BootstrapProcessor.CASCADE_RES_SUFFIX) -> {
                    processCascadeResource(zipFile, entry)
                }
                entryName.endsWith(BootstrapProcessor.XML_SUFFIX) -> {
                    processSurveyFile(zipFile, entry, instanceUrl, awsBucket)
                }
            }
        }
        zipFile.close()
        file.renameTo(File(file.absolutePath + BootstrapProcessor.PROCESSED_OK_SUFFIX))
    }

    private fun processSurveyFile(
        zipFile: ZipFile,
        entry: ZipEntry,
        instanceUrl: String,
        awsBucket: String,
    ) {
        val inputStream = zipFile.getInputStream(entry)
        val formAndMeta = dataFormMapper.mapFormAndMetadata(xmlParser.parseXmlFormWithMeta(inputStream))

        //verify form
        val surveyMetadata = formAndMeta.second
        if (surveyMetadata.alias.isNotEmpty()) {
            if (!instanceUrl.contains(surveyMetadata.alias)) {
                throw WrongDashboardError()
            }
        } else if (surveyMetadata.app.isNotEmpty()) {
            if (!awsBucket.contains(surveyMetadata.app)) {
                throw WrongDashboardError()
            }
        } else {
            throw WrongDashboardError()
        }

        //extract form to app folder
        dataSourceFactory.fileDataSource.copyFormFile(zipFile, entry, formAndMeta.first.formId)
        dataSourceFactory.dataBaseDataSource.insertSurveyGroup(surveyMetadata.survey)
        saveFormAndGroups(formAndMeta.first, true)
    }

    private fun processCascadeResource(zipFile: ZipFile, entry: ZipEntry) {
        return try {
            dataSourceFactory.fileDataSource.extractZipEntry(zipFile, entry, DIR_RES)
        } catch (e: Exception) {
            Timber.e(e)
            throw CascadeProcessingError(e)
        }
    }

    private fun parseFormQuestions(formId: String): HashMap<Int, MutableList<Question>> {
        return xmlParser.parseXmlQuestions(dataSourceFactory.fileDataSource.getFormFile(formId))
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
        val map = dataSurveyMapper.map(apiFormHeader)
        return dataSourceFactory.dataBaseDataSource.insertSurveyGroup(map)
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
        return downloadAndExtractFile(apiFormHeader.id + ZIP_SUFFIX, DIR_FORMS)
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
        val inputStream = dataSourceFactory.fileDataSource.getFormFile(apiFormHeader.id)
        val backUpVersion = apiFormHeader.version.toDouble()
        val form = dataFormMapper.mapForm(xmlParser.parseXmlForm(inputStream, backUpVersion))
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
        dataBaseDataSource.saveQuestionGroups(form)
        return Observable.just(true)
    }

    private fun downloadResources(form: DataForm): Observable<Boolean> {
        return Observable.fromIterable(form.getResources())
            .concatMap { resource ->
                downloadAndExtractFile(resource + ZIP_SUFFIX, DIR_RES)
            }
            .toList()
            .toObservable()
            .map { true }
    }

    companion object {
        private const val TEST_FORM_ID = "0"
    }

}