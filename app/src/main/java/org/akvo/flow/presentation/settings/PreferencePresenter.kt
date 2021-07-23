/*
 * Copyright (C) 2017-2018,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.settings

import org.akvo.flow.domain.entity.UserSettings
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.SaveEnableMobileData
import org.akvo.flow.domain.interactor.SaveImageSize
import org.akvo.flow.domain.interactor.SaveKeepScreenOn
import org.akvo.flow.domain.interactor.UseCase
import org.akvo.flow.domain.interactor.forms.DownloadForm
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.util.logging.LoggingHelper
import timber.log.Timber
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class PreferencePresenter @Inject constructor(
    @param:Named("getUserSettings") private val getUserSettings: UseCase,
    @param:Named("saveEnableMobileData") private val saveEnableMobileData: UseCase,
    @param:Named("saveImageSize") private val saveImageSize: UseCase,
    @param:Named("saveKeepScreenOn") private val saveKeepScreenOn: UseCase,
    @param:Named("unSyncedTransmissionsExist") private val unSyncedTransmissionsExist: UseCase,
    @param:Named("clearResponses") private val clearResponses: UseCase,
    @param:Named("clearAllData") private val clearAllData: UseCase,
    @param:Named("downloadForm") private val downloadForm: UseCase,
    @param:Named("reloadForms") private val reloadForms: UseCase,
    private val mapper: ViewUserSettingsMapper, private val helper: LoggingHelper
) : Presenter {

    private var view: PreferenceView? = null

    fun setView(view: PreferenceView?) {
        this.view = view
    }

    fun loadPreferences() {
        view?.showLoading()
        getUserSettings.execute(object : DefaultObserver<UserSettings?>() {
            override fun onNext(userSettings: UserSettings) {
                val viewUserSettings = mapper
                    .transform(userSettings)
                view?.hideLoading()
                view?.displaySettings(viewUserSettings)
            }
        }, null)
    }

    fun saveEnableMobileData(enable: Boolean) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[SaveEnableMobileData.PARAM_ENABLE_MOBILE_DATA] = enable
        saveEnableMobileData.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
            }
        }, params)
    }

    fun saveImageSize(size: Int) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[SaveImageSize.PARAM_IMAGE_SIZE] = size
        saveImageSize.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
            }
        }, params)
    }

    fun saveKeepScreenOn(enable: Boolean) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[SaveKeepScreenOn.PARAM_KEEP_SCREEN_ON] = enable
        saveKeepScreenOn.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
            }
        }, params)
    }

    override fun destroy() {
        getUserSettings.dispose()
        saveEnableMobileData.dispose()
        saveImageSize.dispose()
        saveKeepScreenOn.dispose()
        unSyncedTransmissionsExist.dispose()
        clearAllData.dispose()
        clearResponses.dispose()
        downloadForm.dispose()
        reloadForms.dispose()
    }

    fun deleteCollectedData() {
        unSyncedTransmissionsExist.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showDeleteCollectedData()
            }

            override fun onNext(exist: Boolean) {
                if (exist) {
                    view?.showDeleteCollectedDataWithPending()
                } else {
                    view?.showDeleteCollectedData()
                }
            }
        }, null)
    }

    fun deleteAllData() {
        unSyncedTransmissionsExist.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showDeleteAllData()
            }

            override fun onNext(exist: Boolean) {
                if (exist) {
                    view?.showDeleteAllDataWithPending()
                } else {
                    view?.showDeleteAllData()
                }
            }
        }, null)
    }

    fun deleteResponsesConfirmed() {
        clearResponses.execute(ClearDataObserver(), null)
    }

    fun deleteAllConfirmed() {
        helper.clearUser()
        clearAllData.execute(ClearDataObserver(), null)
    }

    fun downloadForm(formId: String) {
        view?.showLoading()
        val params: MutableMap<String, Any> = HashMap(2)
        params[DownloadForm.FORM_ID_PARAM] = formId
        downloadForm.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.hideLoading()
                view?.showDownloadFormsError(1)
            }

            override fun onNext(aBoolean: Boolean) {
                view?.hideLoading()
                view?.showDownloadFormsSuccess(1)
            }
        }, params)
    }

    fun reloadForms() {
        view?.showLoading()
        reloadForms.execute(object : DefaultObserver<Int?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.hideLoading()
                view?.showDownloadFormsError(5) //random number for now
            }

            override fun onNext(numberOfForms: Int) {
                view?.hideLoading()
                view?.showDownloadFormsSuccess(numberOfForms)
            }
        }, null)
    }

    private inner class ClearDataObserver : DefaultObserver<Boolean?>() {
        override fun onError(e: Throwable) {
            view?.showClearDataError()
        }

        override fun onNext(cleared: Boolean) {
            if (cleared) {
                view?.showClearDataSuccess()
                view?.dismiss()
            } else {
                view?.showClearDataError()
            }
        }
    }
}
