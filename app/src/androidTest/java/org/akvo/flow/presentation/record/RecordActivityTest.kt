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
import android.util.Pair
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.internal.schedulers.TrampolineScheduler
import it.cosenonjaviste.daggermock.DaggerMock
import org.akvo.flow.R
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.form.data.SurveyInstaller
import org.akvo.flow.activity.form.data.SurveyRequisite
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.domain.entity.User
import org.akvo.flow.domain.executor.SchedulerCreator
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.module.ApplicationModule
import org.akvo.flow.presentation.ScreenRobot
import org.akvo.flow.presentation.ScreenRobot.Companion.withRobot
import org.akvo.flow.presentation.datapoints.map.one.DataPointMapActivity
import org.akvo.flow.service.BootstrapService
import org.akvo.flow.tests.R.raw.data
import org.akvo.flow.util.ConstantUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecordActivityTest {

    @get:Rule
    val rule = espressoDaggerMockRule()

    @get:Rule
    var intentsTestRule: IntentsTestRule<RecordActivity> = object : IntentsTestRule<RecordActivity>(
        RecordActivity::class.java, false, false
    ) {
        override fun getActivityIntent(): Intent {
            val targetContext: Context = getInstrumentation().targetContext
            val result = Intent(targetContext, RecordActivity::class.java)
            result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, SurveyGroup(155852013L, "", "", true))

            val dataPointId = setUpFormData(targetContext)
            result.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId.toString())
            return result
        }
    }

    val surveyRepository = mock(SurveyRepository::class.java)
    val userRepository = mock(UserRepository::class.java)
    val dataPoint = mock(DataPoint::class.java)
    val schedulerCreator = mock(SchedulerCreator::class.java)

    @Before
    fun beforeClass() {
        `when`(surveyRepository.getFormMeta(anyString())).thenReturn(Single.just(Pair(true, "1.0")))
        `when`(surveyRepository.getDataPoint(anyString())).thenReturn(Single.just(dataPoint))
        `when`(userRepository.selectedUser).thenReturn(Observable.just(1L))
        `when`(surveyRepository.getUser(1L)).thenReturn(Observable.just(User(1L, "test_user")))
        `when`(
            surveyRepository.fetchSurveyInstance(
                anyString(),
                anyString(),
                anyString(),
                anyLong(),
                anyString()
            )
        ).thenReturn(Single.just(1L))
        `when`(dataPoint.latitude).thenReturn(41.3819219)
        `when`(dataPoint.longitude).thenReturn(2.148909)
        `when`(schedulerCreator.obtainScheduler()).thenReturn(TrampolineScheduler.instance())
    }

    @Test
    fun activityShouldDisplayCorrectDataPointTitle() {
        `when`(dataPoint.name).thenReturn(DATAPOINT_NAME)

        intentsTestRule.launchActivity(null)

        withRobot(RecordScreenRobot::class.java).checkTitleIs("test datapoint")
    }

    @Test
    fun activityShouldDisplayDefaultDataPointTitleWhenEmptyName() {
        `when`(dataPoint.name).thenReturn("")

        intentsTestRule.launchActivity(null)

        withRobot(RecordScreenRobot::class.java)
            .provideActivityContext(intentsTestRule.activity)
            .checkTitleIs(R.string.unknown)
    }

    @Test
    fun activityShouldDisplayDefaultDataPointTitleWhenNullName() {
        `when`(dataPoint.name).thenReturn(null)

        intentsTestRule.launchActivity(null)

        withRobot(RecordScreenRobot::class.java)
            .provideActivityContext(intentsTestRule.activity)
            .checkTitleIs(R.string.unknown)
    }

    @Test
    fun onFormClickShouldLaunchFormActivityWhenCorrectDataPoint() {
        `when`(dataPoint.name).thenReturn(DATAPOINT_NAME)

        intentsTestRule.launchActivity(null)

        intentsTestRule.activity.onFormClick(FORM_ID)

        withRobot(RecordScreenRobot::class.java).checkFormActivityDisplayed()
    }

    @Test
    fun onFormClickShouldShowErrorMessageWhenBootstrapPending() {
        BootstrapService.isProcessing = true
        `when`(dataPoint.name).thenReturn(DATAPOINT_NAME)

        intentsTestRule.launchActivity(null)

        intentsTestRule.activity.onFormClick(FORM_ID)

        withRobot(RecordScreenRobot::class.java)
            .checkSnackBarDisplayedWithText(R.string.pleasewaitforbootstrap)
        BootstrapService.isProcessing = false
    }

    @Test
    fun onFormClickShouldShowErrorMessageCascadeMissing() {
        `when`(dataPoint.name).thenReturn(DATAPOINT_NAME)
        `when`(surveyRepository.getFormMeta(anyString()))
            .thenReturn(Single.just(Pair(false, "1.0")))

        intentsTestRule.launchActivity(null)

        intentsTestRule.activity.onFormClick(FORM_ID)

        withRobot(RecordScreenRobot::class.java)
            .checkSnackBarDisplayedWithText(R.string.error_missing_cascade)
    }

    @Test
    fun onViewMapShouldOpenMapActivityForCorrectDataPoint() {
        `when`(dataPoint.name).thenReturn(DATAPOINT_NAME)

        intentsTestRule.launchActivity(null)

        withRobot(RecordScreenRobot::class.java).clickMapMenuOption().checkMapActivityDisplayed()
    }

    private fun espressoDaggerMockRule() =
        DaggerMock.rule<ApplicationComponent>(ApplicationModule(app)) {
            set { component -> app.applicationComponent = component }
        }

    val app: FlowApp get() = getInstrumentation().targetContext.applicationContext as FlowApp

    //TODO: remove once formActivity can have mocked dependencies
    private fun setUpFormData(targetContext: Context): Long {
        SurveyRequisite.setRequisites(targetContext)
        val installer = SurveyInstaller(targetContext)
        val survey =
            installer.installSurvey(
                org.akvo.flow.tests.R.raw.all_questions_form,
                getInstrumentation().context
            )
        installer.createDataPoint(
            survey.surveyGroup,
            *SurveyInstaller.generateRepeatedOneGroupResponseData()
        ).first!!
        val dataPointFromFile =
            installer
                .createDataPointFromFile(survey.surveyGroup, getInstrumentation().context, data)
        return dataPointFromFile.first!!
    }

    companion object {
        const val FORM_ID = "156792013"
        const val DATAPOINT_NAME = "test datapoint"
    }

    class RecordScreenRobot : ScreenRobot<RecordScreenRobot>() {

        fun checkMapActivityDisplayed(): RecordScreenRobot {
            return checkActivityDisplayed(DataPointMapActivity::class.java.name)
        }

        fun checkFormActivityDisplayed(): RecordScreenRobot {
            return checkActivityDisplayed(FormActivity::class.java.name)
        }

        fun clickMapMenuOption(): RecordScreenRobot {
            clickOnViewWithId(R.id.more_submenu)
            clickOnViewWithText(R.string.view_map)
            return this
        }
    }
}
