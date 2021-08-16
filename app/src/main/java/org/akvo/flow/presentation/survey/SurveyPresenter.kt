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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.akvo.flow.BuildConfig
import org.akvo.flow.utils.entity.SurveyGroup
import org.akvo.flow.domain.entity.ApkData
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.interactor.apk.GetApkDataPreferences
import org.akvo.flow.domain.interactor.apk.SaveApkUpdateNotified
import org.akvo.flow.domain.interactor.datapoints.MarkDatapointViewed
import org.akvo.flow.domain.interactor.forms.GetFormInstance
import org.akvo.flow.domain.interactor.forms.GetRegistrationForm
import org.akvo.flow.domain.interactor.forms.GetSavedFormInstance
import org.akvo.flow.domain.interactor.users.GetSelectedUser
import org.akvo.flow.domain.interactor.users.ResultCode
import org.akvo.flow.domain.util.VersionHelper
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.entity.ViewApkMapper
import org.akvo.flow.util.ConstantUtil
import java.util.HashMap
import javax.inject.Inject

class SurveyPresenter @Inject constructor(
    private val getApkDataPreferences: GetApkDataPreferences,
    private val saveApkUpdateNotified: SaveApkUpdateNotified,
    private val versionHelper: VersionHelper,
    private val viewApkMapper: ViewApkMapper,
    private val getSelectedUser: GetSelectedUser,
    private val markDatapointViewed: MarkDatapointViewed,
    private val getRegistrationForm: GetRegistrationForm,
    private val getFormInstance: GetFormInstance
) : Presenter {

    private var view: SurveyView? = null
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun destroy() {
        uiScope.coroutineContext.cancelChildren()
    }

    fun setView(view: SurveyView?) {
        this.view = view
    }

    fun verifyApkUpdate() {
        uiScope.launch {
            val result = getApkDataPreferences.execute()
            showApkUpdateIfNeeded(result.apkData, result.notificationTime)
        }
    }

    private fun showApkUpdateIfNeeded(apkData: ApkData?, lastNotified: Long) {
        if (ApkData.NOT_SET_VALUE != apkData && shouldNotifyNewVersion(lastNotified)
            && versionHelper.isNewerVersion(BuildConfig.VERSION_NAME, apkData!!.version)
        ) {
            notifyNewVersionAvailable(apkData)
        }
    }

    private fun notifyNewVersionAvailable(apkData: ApkData?) {
        uiScope.launch {
            saveApkUpdateNotified.execute()
            view?.showNewVersionAvailable(viewApkMapper.transform(apkData))
        }
    }

    private fun shouldNotifyNewVersion(lastNotified: Long): Boolean {
        return if (lastNotified == NOT_NOTIFIED) {
            true
        } else {
            System.currentTimeMillis() - lastNotified >= ConstantUtil.UPDATE_NOTIFICATION_DELAY_IN_MS
        }
    }

    fun onDatapointSelected(datapointId: String, survey: SurveyGroup?) {
        uiScope.launch {
            val userResult = getSelectedUser.execute()
            if (userResult.resultCode == ResultCode.SUCCESS) {
                if (survey != null && survey.isMonitored) {
                    //show a list of forms/submissions
                    view?.displayRecord(datapointId)
                    markDataPointViewed(datapointId)
                } else {
                    //only one form possible
                    val domainForm = fetchRegistrationForm(survey)
                    if (domainForm != null) {
                        if (domainForm.cascadeDownloaded) {
                            markDataPointViewed(datapointId)
                            val params: MutableMap<String, Any> = HashMap(4)
                            params[GetSavedFormInstance.PARAM_FORM_ID] = domainForm.formId
                            params[GetSavedFormInstance.PARAM_DATAPOINT_ID] = datapointId
                            val result = getFormInstance.execute(params)
                            if (result is GetFormInstance.GetFormInstanceResult.GetFormInstanceResultSuccess) {
                                //instance exists, open it
                                view?.navigateToForm(datapointId,
                                    result.surveyInstanceId,
                                    result.readOnly,
                                    domainForm.formId)
                            } else {
                                //no instance exist yet
                                view?.navigateToForm(domainForm.formId,
                                    userResult.user,
                                    datapointId)
                            }
                        } else {
                            view?.showMissingCascadeError()
                            view?.enableClickListener()
                        }
                    } else {
                        view?.showMissingFormError()
                        view?.enableClickListener()
                    }
                }
            } else {
                view?.showMissingUserError()
                view?.enableClickListener()
            }
        }
    }

    private suspend fun markDataPointViewed(datapointId: String) {
        val params: MutableMap<String?, Any> = HashMap(2)
        params[MarkDatapointViewed.PARAM_DATAPOINT_ID] = datapointId
        markDatapointViewed.execute(params)
    }

    fun onAddDataPointTap(surveyGroup: SurveyGroup) {
        uiScope.launch {
            val userResult = getSelectedUser.execute()
            if (userResult.resultCode == ResultCode.SUCCESS) {
                val domainForm = fetchRegistrationForm(surveyGroup)
                if (domainForm != null) {
                    if (domainForm.cascadeDownloaded) {
                        view?.openEmptyForm(userResult.user, domainForm.formId)
                    } else {
                        view?.showMissingCascadeError()
                    }
                } else {
                    view?.showMissingFormError()
                }
            } else {
                view?.showMissingUserError()
            }
        }
    }

    private suspend fun fetchRegistrationForm(survey: SurveyGroup?): DomainForm? {
        if (survey != null) {
            val params: MutableMap<String, Any> = HashMap(2)
            params[GetRegistrationForm.PARAM_SURVEY_ID] = survey.id
            params[GetRegistrationForm.PARAM_REGISTRATION_FORM_ID] =
                survey.registerSurveyId ?: ""
            return getRegistrationForm.execute(params).form
        }
        return null
    }

    fun checkSelectedUser() {
        uiScope.launch {
            val userResult = getSelectedUser.execute()
            if (userResult.resultCode == ResultCode.SUCCESS) {
                view?.displaySelectedUser(userResult.user.name!!)
            }
        }
    }

    companion object {
        private const val NOT_NOTIFIED: Long = -1
    }
}
