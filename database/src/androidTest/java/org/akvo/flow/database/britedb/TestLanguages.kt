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

package org.akvo.flow.database.britedb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.akvo.flow.database.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestLanguages {

    lateinit var languages: Array<String>
    lateinit var codes: Array<String>

    @Before
    fun setUp() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        languages = resources.getStringArray(R.array.alllanguages)
        codes = resources.getStringArray(R.array.alllanguagecodes)
    }

    @Test
    fun bothLanguageArraysHaveSameSize() {
        assertEquals(languages.size, codes.size)
    }

    @Test
    fun checkBembaLanguage() {
        assertEquals("bem", codes[186])
        assertEquals("Bemba", languages[186])
    }

    @Test
    fun checkEnglishLanguage() {
        assertEquals("en", codes[0])
        assertEquals("English", languages[0])
    }
}