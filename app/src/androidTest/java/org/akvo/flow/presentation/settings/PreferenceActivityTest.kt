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
package org.akvo.flow.presentation.settings

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import io.reactivex.internal.schedulers.TrampolineScheduler
import it.cosenonjaviste.daggermock.DaggerMock
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.executor.SchedulerCreator
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.module.ApplicationModule
import org.akvo.flow.presentation.ScreenRobot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferenceActivityTest {

    @get:Rule
    val rule = espressoDaggerMockRule()

    @get:Rule
    var intentsTestRule: IntentsTestRule<PreferenceActivity> =
        object :
            IntentsTestRule<PreferenceActivity>(PreferenceActivity::class.java, false, false) {}

    private fun espressoDaggerMockRule() =
        DaggerMock.rule<ApplicationComponent>(ApplicationModule(app)) {
            set { component -> app.applicationComponent = component }
        }

    val app: FlowApp get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlowApp

    private val schedulerCreator: SchedulerCreator = Mockito.mock(SchedulerCreator::class.java)

    @Before
    fun beforeClass() {
        Mockito.`when`(schedulerCreator.obtainScheduler())
            .thenReturn(TrampolineScheduler.instance())
    }

    @Test
    fun activityShouldDisplayCorrectDataPointTitle() {
        intentsTestRule.launchActivity(null)

        ScreenRobot.withRobot(PreferenceScreenRobot::class.java)
            .provideActivityContext(intentsTestRule.activity)
            .checkTitleIs(R.string.settings)
    }

    @Test
    fun activityShouldDisplayCorrectBasicSettings() {
        intentsTestRule.launchActivity(null)

        ScreenRobot.withRobot(PreferenceScreenRobot::class.java)
            .checkCheckboxIsChecked(R.id.switch_screen_on)
        ScreenRobot.withRobot(PreferenceScreenRobot::class.java)
            .checkCheckboxIsUnChecked(R.id.switch_enable_data)
        ScreenRobot.withRobot(PreferenceScreenRobot::class.java)
            .checkMatchesSpinnerText(
                R.id.preference_image_size,
                intentsTestRule.activity.resources.getStringArray(R.array.max_image_size_pref)[0]
            )
    }

    class PreferenceScreenRobot : ScreenRobot<PreferenceScreenRobot>()
}
