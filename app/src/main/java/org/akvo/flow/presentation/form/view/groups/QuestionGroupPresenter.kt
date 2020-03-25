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

package org.akvo.flow.presentation.form.view.groups

import io.reactivex.observers.DisposableCompletableObserver
import org.akvo.flow.domain.interactor.datapoints.DownloadMedia
import org.akvo.flow.presentation.Presenter
import java.util.HashMap
import javax.inject.Inject

class QuestionGroupPresenter @Inject constructor(private val downloadMediaUseCase: DownloadMedia) :
    Presenter {
    private var groupView: QuestionGroupView? = null

    override fun destroy() {
        downloadMediaUseCase.dispose()
    }

    fun setView(groupView: QuestionGroupView) {
        this.groupView = groupView
    }

    fun downloadMedia(filename: String, viewIndex: Int) {
        val params: MutableMap<String?, Any> = HashMap(2)
        params[DownloadMedia.PARAM_FILE_PATH] = filename
        downloadMediaUseCase.execute(object :
            DisposableCompletableObserver() {
            override fun onComplete() {
                groupView?.showDownloadSuccess(viewIndex)
            }

            override fun onError(e: Throwable) {
                groupView?.showDownloadFailed(viewIndex)
            }
        }, params)
    }
}
