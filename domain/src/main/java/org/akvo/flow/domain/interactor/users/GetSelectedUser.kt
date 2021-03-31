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
package org.akvo.flow.domain.interactor.users

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.entity.User
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import org.akvo.flow.domain.util.Constants
import timber.log.Timber
import javax.inject.Inject

class GetSelectedUser @Inject constructor(
    private val userRepository: UserRepository,
    private val surveyRepository: SurveyRepository
) {

    suspend fun execute(): SelectedUserResult {
        return withContext(Dispatchers.IO) {
            try {
                val userId = userRepository.selectedUser
                if (Constants.INVALID_USER_ID == userId) {
                    SelectedUserResult(
                        User(Constants.INVALID_USER_ID, null),
                        ResultCode.ERROR_OTHER
                    )
                } else {
                    val user = surveyRepository.getUser(userId)
                    if (user.name == null) {
                        SelectedUserResult(user, ResultCode.ERROR_USER_NAME)
                    } else {
                        SelectedUserResult(user, ResultCode.SUCCESS)
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                SelectedUserResult(User(Constants.INVALID_USER_ID, null), ResultCode.ERROR_OTHER)
            }
        }
    }
}
