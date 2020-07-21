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

package org.akvo.flow.domain.entity
//TODO: this is not right separate between 2 different objects: Form from database, form from xml??
data class DomainForm(
    val id: Int = 0,
    val formId: String = "",
    val surveyId: Int = 0,
    val name: String,
    val version: String,
    val type: String = "",
    val location: String = "",
    val filename: String = "",
    val language: String = "en",
    val cascadeDownloaded: Boolean = true,
    val deleted: Boolean = false,
    val title: String = "",
    val groups: List<DomainQuestionGroup> = emptyList()
)
