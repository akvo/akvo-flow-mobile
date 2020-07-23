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

package org.akvo.flow.domain.interactor

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.akvo.flow.domain.entity.FormInstanceMetadata
import org.akvo.flow.domain.repository.FileRepository
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import org.akvo.flow.domain.util.TextValueCleaner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExportSurveyInstancesTest {

    @Mock
    internal var mockUserRepository: UserRepository? = null

    @Mock
    internal var mockValueCleaner: TextValueCleaner? = null

    @Mock
    internal var mockSurveyRepository: SurveyRepository? = null

    @Mock
    internal var mockFileRepository: FileRepository? = null

    @Mock
    internal var mockFormInstanceMetadata: FormInstanceMetadata? = null

    @Before
    fun setUp() {
        `when`(mockUserRepository!!.deviceId).thenReturn(Observable.just("123"))
        `when`(mockValueCleaner!!.cleanVal("123")).thenReturn("123")
        `when`(mockSurveyRepository!!.setInstanceStatusToRequested(anyLong())).thenReturn(
            Completable.complete()
        )
        `when`(mockSurveyRepository!!.getFormInstanceData(anyLong(), anyString())).thenReturn(
            Single.just(
                mockFormInstanceMetadata
            )
        )
        `when`(
            mockFileRepository!!.createDataZip(
                any(),
                any()
            )
        ).thenReturn(Completable.complete())
        `when`(
            mockSurveyRepository!!.createTransmissions(
                anyLong(),
                anyString(),
                anySet<String>()
            )
        ).thenReturn(Completable.complete())

        `when`(mockFormInstanceMetadata!!.zipFileName).thenReturn("")
        `when`(mockFormInstanceMetadata!!.formInstanceData).thenReturn("")
        `when`(mockFormInstanceMetadata!!.formId).thenReturn("")
    }

    @Test
    fun buildUseCaseObservableShouldCompleteCorrectlyFor7Instances() {
        `when`(mockSurveyRepository!!.pendingSurveyInstances).thenReturn(
            Single.just(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L))
        )

        val observer = TestObserver<Void>()

        val exportSurveyInstances =
            ExportSurveyInstances(
                mockUserRepository,
                mockValueCleaner,
                mockSurveyRepository,
                mockFileRepository
            )

        exportSurveyInstances.buildUseCaseObservable().subscribe(observer)

        verify(mockSurveyRepository, times(7))!!.setInstanceStatusToRequested(anyLong())
        verify(mockSurveyRepository, times(7))!!.getFormInstanceData(anyLong(), anyString())
        verify(mockFileRepository, times(7))!!.createDataZip(anyString(), anyString())
        verify(mockSurveyRepository, times(7))!!.createTransmissions(
            anyLong(),
            anyString(),
            anySet<String>()
        )
        observer.assertNoErrors()
    }

    @Test
    fun buildUseCaseObservableShouldCompleteCorrectlyFor0Instances() {
        `when`(mockSurveyRepository!!.pendingSurveyInstances).thenReturn(
            Single.just(emptyList())
        )

        val observer = TestObserver<Void>()

        val exportSurveyInstances =
            ExportSurveyInstances(
                mockUserRepository,
                mockValueCleaner,
                mockSurveyRepository,
                mockFileRepository
            )

        exportSurveyInstances.buildUseCaseObservable().subscribe(observer)

        verify(mockSurveyRepository, times(0))!!.setInstanceStatusToRequested(anyLong())
        verify(mockSurveyRepository, times(0))!!.getFormInstanceData(anyLong(), anyString())
        verify(mockFileRepository, times(0))!!.createDataZip(anyString(), anyString())
        verify(mockSurveyRepository, times(0))!!.createTransmissions(
            anyLong(),
            anyString(),
            anySet<String>()
        )
        observer.assertNoErrors()
    }
}
