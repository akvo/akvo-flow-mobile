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
package org.akvo.flow.presentation.datapoints

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DisplayNameMapperTest {

    private var mockContext = mock(Context::class.java)

    @Before
    fun setUp() {
        `when`(mockContext.getString(anyInt())).thenReturn("Unnamed data point")
    }

    @Test
    fun createDisplayNameShouldReturnCorrectName() {
        val displayNameMapper = DisplayNameMapper(mockContext);

        val name = displayNameMapper.createDisplayName("datapoint1")

        assertEquals("datapoint1", name)
    }

    @Test
    fun createDisplayNameShouldReturnCorrectNameForNullName() {
        val displayNameMapper = DisplayNameMapper(mockContext);

        val name = displayNameMapper.createDisplayName(null)

        assertEquals("Unnamed data point", name)
    }

    @Test
    fun createDisplayNameShouldReturnCorrectNameForEmptyName() {
        val displayNameMapper = DisplayNameMapper(mockContext);

        val name = displayNameMapper.createDisplayName("")

        assertEquals("Unnamed data point", name)
    }
}