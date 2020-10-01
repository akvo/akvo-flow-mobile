/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.datapoints.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.domain.entity.DownloadResult
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.DownloadDataPoints
import org.akvo.flow.domain.interactor.UseCase
import org.akvo.flow.domain.interactor.datapoints.GetSavedDataPoints
import org.akvo.flow.domain.util.Constants
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.datapoints.list.entity.DataPointStatus
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPointMapper
import org.akvo.flow.util.ConstantUtil
import timber.log.Timber
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class DataPointsListPresenter @Inject internal constructor(
    @param:Named("getSavedDataPoints") private val getSavedDataPoints: UseCase,
    private val mapper: ListDataPointMapper, private val downloadDataPoints: DownloadDataPoints,
    @param:Named("checkDeviceNotification") private val checkDeviceNotification: UseCase,
    @param:Named("uploadSync") private val upload: UseCase
) : Presenter {

    private var view: DataPointsListView? = null
    private var surveyGroup: SurveyGroup? = null
    private var orderBy = ConstantUtil.ORDER_BY_DATE
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    fun setView(view: DataPointsListView) {
        this.view = view
    }

    fun onDataReady(surveyGroup: SurveyGroup?) {
        this.surveyGroup = surveyGroup
        if (surveyGroup == null) {
            view?.hideMenu()
        } else {
            if (surveyGroup.isMonitored) {
                view?.showMonitoredMenu()
            } else {
                view?.showNonMonitoredMenu()
            }
        }
    }

    fun loadDataPoints(latitude: Double?, longitude: Double?) {
        this.latitude = latitude
        this.longitude = longitude
        getSavedDataPoints.dispose()
        if (surveyGroup != null) {
            val params: MutableMap<String, Any?> = HashMap(8)
            params[GetSavedDataPoints.KEY_SURVEY_GROUP_ID] = surveyGroup!!.id
            params[GetSavedDataPoints.KEY_ORDER_BY] = orderBy
            params[GetSavedDataPoints.KEY_LATITUDE] = latitude
            params[GetSavedDataPoints.KEY_LONGITUDE] = longitude
            getSavedDataPoints.execute<List<DataPoint>>(
                object : DefaultObserver<List<DataPoint>>() {
                    override fun onError(e: Throwable) {
                        Timber.e(e, "Error loading saved datapoints")
                        view?.displayData(emptyList())
                        view?.showNoDataPoints(surveyGroup!!.isMonitored)
                    }

                    override fun onNext(dataPoints: List<DataPoint>) {
                        val mapDataPoints =
                            mapper.transform(dataPoints, latitude, longitude)
                        view?.displayData(mapDataPoints)
                        if (mapDataPoints.isEmpty()) {
                            view?.showNoDataPoints(surveyGroup!!.isMonitored)
                        }
                    }
                },
                params
            )
        } else {
            noSurveySelected()
        }
    }

    fun getFilteredDataPoints(filter: String?) {
        getSavedDataPoints.dispose()
        if (surveyGroup != null) {
            val params: MutableMap<String, Any?> = HashMap(8)
            params[GetSavedDataPoints.KEY_SURVEY_GROUP_ID] = surveyGroup!!.id
            params[GetSavedDataPoints.KEY_ORDER_BY] = orderBy
            params[GetSavedDataPoints.KEY_LATITUDE] = latitude
            params[GetSavedDataPoints.KEY_LONGITUDE] = longitude
            params[GetSavedDataPoints.KEY_FILTER] = filter
            getSavedDataPoints.execute<List<DataPoint>>(
                object :
                    DefaultObserver<List<DataPoint>>() {
                    override fun onError(e: Throwable) {
                        Timber.e(e, "Error loading saved datapoints")
                        view?.displayData(emptyList())
                        view?.displayNoSearchResultsFound()
                    }

                    override fun onNext(dataPoints: List<DataPoint>) {
                        val listDataPoints = mapper.transform(dataPoints, latitude, longitude)
                        view?.displayData(listDataPoints)
                        if (listDataPoints.isEmpty()) {
                            view?.displayNoSearchResultsFound()
                        }
                    }
                },
                params
            )
        } else {
            noSurveySelected()
        }
    }

    override fun destroy() {
        getSavedDataPoints.dispose()
        checkDeviceNotification.dispose()
        upload.dispose()
        uiScope.coroutineContext.cancelChildren()
    }

    fun onDownloadPressed() {
        if (surveyGroup != null) {
            view?.showLoading()
            uiScope.launch {
                val params: MutableMap<String, Any> = HashMap(2)
                params[DownloadDataPoints.KEY_SURVEY_ID] = surveyGroup!!.id
                val result: DownloadResult = downloadDataPoints.execute(params)
                view?.hideLoading()
                if (result.resultCode == DownloadResult.ResultCode.SUCCESS) {
                    if (result.numberOfNewItems > 0) {
                        view?.showDownloadedResults(result.numberOfNewItems)
                    } else {
                        view?.showNoDataPointsToSync()
                    }
                } else {
                    when (result.resultCode) {
                        DownloadResult.ResultCode.ERROR_NO_NETWORK -> view?.showErrorNoNetwork()
                        DownloadResult.ResultCode.ERROR_ASSIGNMENT_MISSING -> view?.showErrorAssignmentMissing()
                        else -> view?.showErrorSync()
                    }
                }
            }
        }
    }

    fun onOrderByClick(order: Int) {
        if (orderBy != order) {
            when {
                order == ConstantUtil.ORDER_BY_DISTANCE && (latitude == null || longitude == null) -> {
                    // Warn user that the location is unknown
                    view?.showErrorMissingLocation()
                    return
                }
                else -> {
                    orderBy = order
                    loadDataPoints(latitude, longitude)
                }
            }
        }
    }

    fun onOrderByClicked() {
        view?.showOrderByDialog(orderBy)
    }

    fun onNewSurveySelected(surveyGroup: SurveyGroup?) {
        getSavedDataPoints.dispose()
        uiScope.coroutineContext.cancelChildren()
        view?.hideLoading()
        onDataReady(surveyGroup)
        loadDataPoints(latitude, longitude)
    }

    fun onUploadPressed() {
        if (surveyGroup != null) {
            view?.showLoading()
            val params: MutableMap<String, Any> = HashMap(2)
            params[Constants.KEY_SURVEY_ID] = surveyGroup!!.id.toString()
            checkDeviceNotification.execute<Set<String>>(object :
                DefaultObserver<Set<String>>() {
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
        upload.execute<Set<String>>(object :
            DefaultObserver<Set<String>>() {
            override fun onError(e: Throwable) {
                view?.hideLoading()
                Timber.e(e)
            }

            override fun onComplete() {
                view?.hideLoading()
            }
        }, params)
    }

    private fun noSurveySelected() {
        view?.displayData(emptyList())
        view?.showNoSurveySelected()
    }

    fun onDataPointClicked(listDatapoint: ListDataPoint) {
        if (listDatapoint.status == DataPointStatus.DOWNLOADING) {
            view?.displayErrorDataPointDownloading()
        } else {
            view?.onDataPointClicked(listDatapoint.id)
        }
    }
}
