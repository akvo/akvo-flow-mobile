/*
 * Copyright (C) 2018-2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.survey

import androidx.core.util.Pair
import io.reactivex.observers.DisposableCompletableObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.akvo.flow.BuildConfig
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.ApkData
import org.akvo.flow.domain.entity.User
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.UseCase
import org.akvo.flow.domain.interactor.datapoints.MarkDatapointViewed
import org.akvo.flow.domain.interactor.forms.GetRegistrationForm
import org.akvo.flow.domain.interactor.forms.RegistrationFormResult
import org.akvo.flow.domain.interactor.users.GetSelectedUser
import org.akvo.flow.domain.util.VersionHelper
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.entity.ViewApkMapper
import org.akvo.flow.util.ConstantUtil
import timber.log.Timber
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class SurveyPresenter @Inject constructor(
    @param:Named("GetApkDataPreferences") private val getApkDataPreferences: UseCase,
    @param:Named("SaveApkUpdateNotified") private val saveApkUpdateNotified: UseCase,
    private val versionHelper: VersionHelper,
    private val viewApkMapper: ViewApkMapper,
    private val getSelectedUser: GetSelectedUser,
    private val markDatapointViewed: MarkDatapointViewed,
    private val getRegistrationForm: GetRegistrationForm
) : Presenter {

    private var view: SurveyView? = null
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun destroy() {
        getApkDataPreferences.dispose()
        saveApkUpdateNotified.dispose()
        getSelectedUser.dispose()
        markDatapointViewed.dispose()
        uiScope.coroutineContext.cancelChildren()
    }

    fun setView(view: SurveyView?) {
        this.view = view
    }

    fun verifyApkUpdate() {
        getApkDataPreferences.execute(object :
            DefaultObserver<Pair<ApkData, Long>>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
            }

            override fun onNext(apkDataLongPair: Pair<ApkData, Long>) {
                showApkUpdateIfNeeded(apkDataLongPair.first, apkDataLongPair.second!!)
            }
        }, null)
    }

    private fun showApkUpdateIfNeeded(apkData: ApkData?, lastNotified: Long) {
        if (ApkData.NOT_SET_VALUE != apkData && shouldNotifyNewVersion(lastNotified)
            && versionHelper.isNewerVersion(BuildConfig.VERSION_NAME, apkData!!.version)
        ) {
            notifyNewVersionAvailable(apkData)
        }
    }

    private fun notifyNewVersionAvailable(apkData: ApkData?) {
        saveApkUpdateNotified.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
            }
        }, null)
        view?.showNewVersionAvailable(viewApkMapper.transform(apkData))
    }

    private fun shouldNotifyNewVersion(lastNotified: Long): Boolean {
        return if (lastNotified == NOT_NOTIFIED) {
            true
        } else {
            System.currentTimeMillis() - lastNotified >= ConstantUtil.UPDATE_NOTIFICATION_DELAY_IN_MS
        }

    }

    fun onDatapointSelected(datapointId: String) {
        getSelectedUser.execute(object : DefaultObserver<User?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showMissingUserError()
            }

            override fun onNext(user: User) {
                if (user.name == null) {
                    view?.showMissingUserError()
                } else {
                    setDataPointAsViewed(datapointId, user)
                }
            }
        }, null)
    }

    private fun setDataPointAsViewed(datapointId: String, user: User) {
        val params: MutableMap<String?, Any> = HashMap(2)
        params[MarkDatapointViewed.PARAM_DATAPOINT_ID] = datapointId
        markDatapointViewed.execute(object : DisposableCompletableObserver() {
            override fun onComplete() {
                view?.openDataPoint(datapointId, user)
            }

            override fun onError(e: Throwable) {
                view?.openDataPoint(datapointId, user)
            }
        }, params)
    }

    fun onAddDataPointTap(surveyGroup: SurveyGroup) {
        getSelectedUser.execute(object : DefaultObserver<User>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showMissingUserError()
            }

            override fun onNext(user: User) {
                if (user.name == null) {
                    view?.showMissingUserError()
                } else {
                    uiScope.launch {
                        val params: MutableMap<String, Any> = HashMap(2)
                        params[GetRegistrationForm.PARAM_SURVEY_ID] = surveyGroup.id
                        params[GetRegistrationForm.PARAM_REGISTRATION_FORM_ID] =
                            surveyGroup.registerSurveyId
                        val result: RegistrationFormResult = getRegistrationForm.execute(params)
                        val domainForm = result.form
                        if (domainForm != null) {
                            if (domainForm.cascadeDownloaded) {
                                view?.openEmptyForm(user, domainForm.formId)
                            } else {
                                view?.showMissingCascadeError()
                            }
                        } else {
                            view?.showMissingFormError()
                        }
                    }
                }
            }
        }, null)
    }

    companion object {
        private const val NOT_NOTIFIED: Long = -1
    }
}
