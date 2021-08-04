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

package org.akvo.flow.database.britedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.tables.DataPointDownloadTable;
import org.akvo.flow.database.tables.FormUpdateNotifiedTable;
import org.akvo.flow.database.tables.LanguageTable;
import org.akvo.flow.database.tables.QuestionGroupTable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;

@RunWith(AndroidJUnit4.class)
public class BriteSurveyDbAdapterTest {

    private BriteSurveyDbAdapter adapter;

    @Before
    public void setUp() {
        DatabaseHelper databaseHelper = new DatabaseHelper(InstrumentationRegistry.getInstrumentation().getTargetContext(), new LanguageTable(), new DataPointDownloadTable(), new FormUpdateNotifiedTable(), new QuestionGroupTable());
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(databaseHelper, AndroidSchedulers.mainThread());
        this.adapter = new BriteSurveyDbAdapter(db);
    }

    @Test
    public void cleanDataPointsShouldDeleteEmptyDataPoints() {
        ContentValues values = new ContentValues();

        String dataPointId = UUID.randomUUID().toString();
        Long surveyGroupId = 1L;
        values.put(RecordColumns.RECORD_ID, dataPointId);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(RecordColumns.NAME, "Name");
        values.put(RecordColumns.LATITUDE, 0.0d);
        values.put(RecordColumns.LONGITUDE, 0.0d);
        values.put(RecordColumns.LAST_MODIFIED, new Date().getTime());

        assertTrue(adapter.insertOrUpdateRecord(dataPointId, values));

        adapter.cleanDataPoints(surveyGroupId);

        Cursor cursor = adapter.getDataPoint(dataPointId);

        assertFalse(cursor.moveToFirst());

        cursor.close();
    }

    @Test
    public void cleanDataPointsShouldNotDeleteCompleteDataPoints() {
        ContentValues dataPoint = new ContentValues();

        // Data Point
        String dataPointId = UUID.randomUUID().toString();
        Long surveyGroupId = 1L;
        dataPoint.put(RecordColumns.RECORD_ID, dataPointId);
        dataPoint.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        dataPoint.put(RecordColumns.NAME, "Name");
        dataPoint.put(RecordColumns.LATITUDE, 0.0d);
        dataPoint.put(RecordColumns.LONGITUDE, 0.0d);
        dataPoint.put(RecordColumns.LAST_MODIFIED, new Date().getTime());

        assertTrue(adapter.insertOrUpdateRecord(dataPointId, dataPoint));

        // Form Instance
        String formInstanceUuid = UUID.randomUUID().toString();
        ContentValues formInstance = new ContentValues();
        formInstance.put(SurveyInstanceColumns.SURVEY_ID, 0L);
        formInstance.put(SurveyInstanceColumns.SUBMITTED_DATE, new Date().getTime());
        formInstance.put(SurveyInstanceColumns.RECORD_ID, dataPointId);
        formInstance.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
        formInstance.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
        formInstance.put(SurveyInstanceColumns.SUBMITTER, "Submitter");

        long formInstanceId = adapter.syncSurveyInstance(formInstance, formInstanceUuid);

        // Answer
        ContentValues answer = new ContentValues();
        answer.put(ResponseColumns.ANSWER, "Hola");
        answer.put(ResponseColumns.TYPE, "VALUE");
        answer.put(ResponseColumns.QUESTION_ID, "1");
        answer.put(ResponseColumns.INCLUDE, true);
        answer.put(ResponseColumns.SURVEY_INSTANCE_ID, formInstanceId);

        adapter.syncResponse(formInstanceId, answer, "1");

        adapter.cleanDataPoints(surveyGroupId);

        Cursor cursor = adapter.getDataPoint(dataPointId);

        assertTrue(cursor.moveToFirst());

        assertEquals(dataPointId, cursor.getString(1));

        cursor.close();
    }
}