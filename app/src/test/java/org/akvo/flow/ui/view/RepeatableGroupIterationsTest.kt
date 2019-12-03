/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.ui.view

import org.akvo.flow.domain.QuestionResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatableGroupIterationsTest {

    @Test
    fun loadIDsShouldCreateEmptyListIfEmptyResponses() {
        val iterations = RepeatableGroupIterations()

        iterations.loadIDs(HashSet(), emptyList())

        assertEquals(0, iterations.size())
    }
    
    @Test
    fun loadIDsShouldCreateEmptyListIfQuestionIdIsEmpty() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("")
        builder.setIteration(0)

        iterations.loadIDs(setOf("1234"), listOf(builder.createQuestionResponse()))

        assertEquals(0, iterations.size())
    }

    @Test
    fun loadIDsShouldCreateEmptyListIfQuestionIdNotInSet() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("123")
        builder.setIteration(0)

        iterations.loadIDs(setOf("1234"), listOf(builder.createQuestionResponse()))

        assertEquals(0, iterations.size())
    }

    @Test
    fun loadIDsShouldCreateEmptyListIfIterationInvalid() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("123")
        builder.setIteration(-1)

        iterations.loadIDs(setOf("123"), listOf(builder.createQuestionResponse()))

        assertEquals(0, iterations.size())
    }

    @Test
    fun loadIDsShouldCreateCorrectIteration() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("123")
        builder.setIteration(0)

        iterations.loadIDs(setOf("123"), listOf(builder.createQuestionResponse()))

        assertEquals(1, iterations.size())
        assertEquals(0, iterations.getRepetitionId(0))
    }

    @Test
    fun nextShouldReturn0IfEmptyIterations() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("123")
        builder.setIteration(0)
        iterations.loadIDs(setOf("1234"), listOf(builder.createQuestionResponse()))

        val nextIteration = iterations.next()

        assertEquals(0, nextIteration)
        assertEquals(0, iterations.getRepetitionId(0))
    }

    @Test
    fun nextShouldReturnCorrectIteration() {
        val iterations = RepeatableGroupIterations()
        val builder = QuestionResponse.QuestionResponseBuilder()
        builder.setQuestionId("123")
        builder.setIteration(0)
        iterations.loadIDs(setOf("123"), listOf(builder.createQuestionResponse()))

        val nextIteration = iterations.next()

        assertEquals(1, nextIteration)
        assertEquals(2, iterations.size())
    }
}
