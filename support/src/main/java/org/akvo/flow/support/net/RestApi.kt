/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.support.net

import okhttp3.ResponseBody
import javax.inject.Inject

class RestApi @Inject constructor(private val serviceFactory: RestServiceFactory) {

    suspend fun fetchConversations(page: Int = 0): ConversationsResult {
        return serviceFactory.createReamazeRetrofitService(ReamazeService::class.java)
            .fetchConversations(page)
    }

    /**
     * Replies are visible for everyone
     */
    suspend fun postConversationReply(slug: String, body: String): ResponseBody {
        val response = MessageResponse(Message(body, 0))
        return serviceFactory.createReamazeRetrofitService(ReamazeService::class.java)
            .uploadResponse(slug, response)
    }

    /**
     * Notes can only be viewed by staff members
     */
    suspend fun postConversationNote(slug: String, body: String): ResponseBody {
        val response = MessageResponse(Message(body, 0))
        return serviceFactory.createReamazeRetrofitService(ReamazeService::class.java)
            .uploadResponse(slug, response)
    }
}
