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

package org.akvo.flow.support.domain

import kotlinx.coroutines.withContext
import org.akvo.flow.support.net.Conversation
import org.akvo.flow.support.net.RestApi
import timber.log.Timber
import javax.inject.Inject

class SendConversation @Inject constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val restApi: RestApi,
) {

    suspend fun execute(parameters: Map<String, Any>): SendConversationResult {
        if (!parameters.containsKey(PARAM_REF_ID) || !parameters.containsKey(PARAM_BODY)) {
            throw IllegalArgumentException("Missing ref id and/or body")
        }

        return withContext(coroutineDispatcher.getDispatcher()) {
            //find the slug
            val conversation: Conversation? = findConversation(parameters)
            if (conversation != null) {
                val body = parameters[PARAM_BODY] as String
                val slug = conversation.slug
                try {
                    restApi.postConversationReply(slug, body)
                    SendConversationResult.ResultSuccess
                } catch (e: Exception) {
                    Timber.e(e)
                    SendConversationResult.ErrorSending
                }
            } else {
                SendConversationResult.ErrorConversationNotFound
            }
        }
    }

    private suspend fun findConversation(parameters: Map<String, Any>): Conversation? {
        val refId = parameters[PARAM_REF_ID] as Int
        var page = -1
        var conversation: Conversation?
        do {
            page++
            val conversationsResult = restApi.fetchConversations(page)
            val pageCount = conversationsResult.pageCount
            val conversations = conversationsResult.conversations
            conversation = findConversation(conversations, refId)
        } while (conversation == null && page < pageCount)
        return conversation
    }

    private fun findConversation(conversations: List<Conversation>?, refId: Int): Conversation? {
        return try {
            conversations?.first { it.refId == refId }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    companion object {
        const val PARAM_REF_ID = "ref_if"
        const val PARAM_BODY = "body"
    }

    sealed class SendConversationResult {
        object ResultSuccess : SendConversationResult()
        object ErrorConversationNotFound : SendConversationResult()
        object ErrorSending : SendConversationResult()
    }
}