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

package org.akvo.flow.utils.entity

data class Form(
    val id: Int,
    val formId: String,
    val surveyId: Int,
    val name: String,
    val version: Double,
    val type: String = "survey",
    val location: String = "sdcard",
    val filename: String,
    val language: String = "en",
    val cascadeDownloaded: Boolean = true,
    val deleted: Boolean = false,
    val groups: MutableList<QuestionGroup> = mutableListOf()
) {

    fun getResources(): List<String> {
        val formResources = mutableListOf<String>()
        for (group in groups) {
            for (question in group.questions) {
                val cascadeResource = question.cascadeResource
                if (!cascadeResource.isNullOrEmpty()) {
                    formResources.add(cascadeResource)
                }
            }
        }
        return formResources
    }
}
