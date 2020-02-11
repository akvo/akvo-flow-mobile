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

package org.akvo.flow.presentation.record

import android.content.Context
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import io.reactivex.Single
import it.cosenonjaviste.daggermock.DaggerMock
import org.akvo.flow.R
import org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.module.ApplicationModule
import org.akvo.flow.util.ConstantUtil
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecordActivityTest {

    @get:Rule val rule = espressoDaggerMockRule()

    @get:Rule
    var activityRule: ActivityTestRule<RecordActivity> = object : ActivityTestRule<RecordActivity>(
        RecordActivity::class.java, false, false
    ) {
        override fun getActivityIntent(): Intent {
            val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
            val result = Intent(targetContext, RecordActivity::class.java)
            result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, SurveyGroup(1L, "", "", true))
            result.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, "123")
            return result
        }
    }

    val surveyRepository = mock(SurveyRepository::class.java)
    val userRepository = mock(UserRepository::class.java)
    val dataPoint = mock(DataPoint::class.java)

    @Test
    fun activityShouldDisplayCorrectDataPointTitle() {
        `when`(surveyRepository.getDataPoint(Matchers.anyString())).thenReturn(Single.just(dataPoint))
        `when`(dataPoint.name).thenReturn("test datapoint")

        activityRule.launchActivity(null)

        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(`is`<String>("test datapoint"))))
    }

    @Test
    fun activityShouldDisplayDefaultDataPointTitleWhenNoName() {
        `when`(surveyRepository.getDataPoint(Matchers.anyString())).thenReturn(Single.just(dataPoint))
        `when`(dataPoint.name).thenReturn("")

        activityRule.launchActivity(null)

        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(`is`<String>(app.getString(R.string.unknown)))))
    }

    private fun espressoDaggerMockRule() = DaggerMock.rule<ApplicationComponent>(ApplicationModule(app)) {
        set { component -> app.applicationComponent = component }
    }

    val app: FlowApp get() = getInstrumentation().targetContext.applicationContext as FlowApp
}