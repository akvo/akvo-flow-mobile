/*
 * Copyright (C) 2018,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.domain.interactor.apk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.entity.ApkData
import org.akvo.flow.domain.repository.ApkRepository
import org.akvo.flow.domain.repository.UserRepository
import javax.inject.Inject

class GetApkDataPreferences @Inject constructor(
    private val apkRepository: ApkRepository,
    private val userRepository: UserRepository
) {

    suspend fun execute(): ApkResult {
        return withContext(Dispatchers.IO) {
            try {
                val date = async { apkRepository.apkDataPreference }
                val time = async { userRepository.lastNotificationTime }
                ApkResult(date.await(), time.await(), ResultCode.SUCCESS)
            } catch (e: Exception) {
                ApkResult(ApkData.NOT_SET_VALUE, -1, ResultCode.ERROR)
            }
        }
    }

    data class ApkResult(
        val apkData: ApkData,
        val notificationTime: Long,
        val resultCode: ResultCode
    )

    enum class ResultCode {
        SUCCESS, ERROR
    }
}
