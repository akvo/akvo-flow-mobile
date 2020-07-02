/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.datapoints.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.domain.entity.DownloadResult
import org.akvo.flow.domain.entity.DownloadResult.ResultCode
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.DownloadDataPoints
import org.akvo.flow.domain.interactor.UseCase
import org.akvo.flow.domain.interactor.datapoints.GetSavedDataPoints
import org.akvo.flow.domain.util.Constants
import org.akvo.flow.presentation.Presenter
import timber.log.Timber
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class DataPointsMapPresenter @Inject internal constructor(
    @param:Named("getSavedDataPoints") private val getSavedDataPoints: UseCase,
    private val downloadDataPoints: DownloadDataPoints,
    @param:Named("checkDeviceNotification") private val checkDeviceNotification: UseCase,
    @param:Named("uploadSync") private val upload: UseCase,
    private val featureMapper: FeatureMapper
) : Presenter {
    private var view: DataPointsMapView? = null
    private var surveyGroup: SurveyGroup? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    fun setView(view: DataPointsMapView) {
        this.view = view
    }

    fun onSurveyGroupReady(surveyGroup: SurveyGroup?) {
        this.surveyGroup = surveyGroup
        if (surveyGroup == null) {
            view!!.hideMenu()
        } else {
            if (surveyGroup.isMonitored) {
                view!!.showMonitoredMenu()
            } else {
                view!!.showNonMonitoredMenu()
            }
            view!!.showFab()
        }
    }

    fun loadDataPoints() {
        getSavedDataPoints.dispose()
        if (surveyGroup != null) {
            val params: MutableMap<String, Any> = HashMap(2)
            params[GetSavedDataPoints.KEY_SURVEY_GROUP_ID] = surveyGroup!!.id
            getSavedDataPoints.execute<List<DataPoint>>(
                object : DefaultObserver<List<DataPoint>>() {
                    override fun onError(e: Throwable) {
                        Timber.e(e, "Error loading saved datapoints")
                        view!!.displayDataPoints(featureMapper.getFeatureCollection(ArrayList()))
                    }

                    override fun onNext(dataPoints: List<DataPoint>) {
                        view!!.displayDataPoints(featureMapper.getFeatureCollection(dataPoints))
                    }
                },
                params
            )
        }
    }

    override fun destroy() {
        getSavedDataPoints.dispose()
        downloadDataPoints.dispose()
        checkDeviceNotification.dispose()
        upload.dispose()
    }

    fun onNewSurveySelected(surveyGroup: SurveyGroup?) {
        getSavedDataPoints.dispose()
        downloadDataPoints.dispose()
        view!!.hideProgress()
        onSurveyGroupReady(surveyGroup)
        loadDataPoints()
    }

    fun onSyncRecordsPressed() {
        surveyGroup?.let { surveyGroup ->
            view!!.showProgress()
            uiScope.launch {
                val params: MutableMap<String, Any> = HashMap(2)
                params[DownloadDataPoints.KEY_SURVEY_ID] = surveyGroup.id
                val result: DownloadResult = downloadDataPoints.execute(params)
                view!!.hideProgress()
                if (result.resultCode == ResultCode.SUCCESS) {
                    if (result.numberOfNewItems > 0) {
                        view!!.showDownloadedResults(result.numberOfNewItems)
                    } else {
                        view!!.showNoDataPointsToDownload()
                    }
                } else {
                    when (result.resultCode) {
                        ResultCode.ERROR_NO_NETWORK -> view!!.showErrorNoNetwork()
                        ResultCode.ERROR_ASSIGNMENT_MISSING -> view!!.showErrorAssignmentMissing()
                        else -> view!!.showErrorSync()
                    }
                }
            }
        }
    }

    fun onUploadPressed() {
        if (surveyGroup != null) {
            view!!.showProgress()
            val params: MutableMap<String, Any> = HashMap(2)
            params[Constants.KEY_SURVEY_ID] = surveyGroup!!.id.toString() + ""
            checkDeviceNotification.execute<Set<String>>(object : DefaultObserver<Set<String>>() {
                override fun onError(e: Throwable) {
                    Timber.e(e)
                    uploadDataPoints(params)
                }

                override fun onNext(ignored: Set<String>) {
                    uploadDataPoints(params)
                }
            }, params)
        }
    }

    private fun uploadDataPoints(params: Map<String, Any>) {
        upload.execute<Set<String>>(object : DefaultObserver<Set<String>>() {
            override fun onError(e: Throwable) {
                view!!.hideProgress()
                Timber.e(e)
            }

            override fun onComplete() {
                view!!.hideProgress()
            }
        }, params)
    }
}
