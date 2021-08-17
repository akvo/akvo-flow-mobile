/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.data.database;

import static org.akvo.flow.database.tables.QuestionGroupTable.COLUMN_FORM_ID;
import static org.akvo.flow.database.tables.QuestionGroupTable.COLUMN_GROUP_ID;
import static org.akvo.flow.database.tables.QuestionGroupTable.COLUMN_HEADING;
import static org.akvo.flow.database.tables.QuestionGroupTable.COLUMN_ORDER;
import static org.akvo.flow.database.tables.QuestionGroupTable.COLUMN_REPEATABLE;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.squareup.sqlbrite2.BriteDatabase;

import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.britedb.BriteSurveyDbAdapter;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.utils.entity.Form;
import org.akvo.flow.utils.entity.QuestionGroup;
import org.akvo.flow.utils.entity.SurveyGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Temporary class to access SurveyDb from the app without having to refactor the whole architecture
 */
public class SurveyDbDataSource {

    private final SurveyDbAdapter surveyDbAdapter;
    private final BriteSurveyDbAdapter briteSurveyDbAdapter;
    private final SurveyMapper surveyMapper = new SurveyMapper();
    private final TransmissionsMapper transmissionsMapper = new TransmissionsMapper();

    @Inject
    public SurveyDbDataSource(BriteDatabase briteDatabase, SQLiteOpenHelper databaseHelper) {
        this.briteSurveyDbAdapter = new BriteSurveyDbAdapter(briteDatabase);
        this.surveyDbAdapter = new SurveyDbAdapter(databaseHelper);
    }

    /**
     * Open or create the briteSurveyDbAdapter
     *
     * @throws SQLException if the database could be neither opened or created
     */
    public SurveyDbAdapter open() throws SQLException {
        return surveyDbAdapter.open();
    }

    /**
     * close the db
     */
    public void close() {
        surveyDbAdapter.close();
    }

    private QuestionResponse.QuestionResponseBuilder getQuestionResponseBuilder(Cursor cursor,
          QuestionResponseColumns columns) {
        return new QuestionResponse.QuestionResponseBuilder()
                .setValue(cursor.getString(columns.getAnswerColumn()))
                .setType(cursor.getString(columns.getTypeColumn()))
                .setId(cursor.getLong(columns.getIdColumn()))
                .setQuestionId(cursor.getString(columns.getQuestionIdColumn()))
                .setIncludeFlag(cursor.getInt(columns.getIncludeColumn()) == 1)
                .setIteration(cursor.getInt(columns.getIterationColumn()));
    }

    /**
     *  Adapt(clone) responses for the current survey instance:
     *  Get rid of its Id and update the SurveyInstance Id
     */
    public Map<String, QuestionResponse> getResponsesForPreFilledSurvey(long surveyInstanceId,
            long newSurveyInstanceId) {
        Map<String, QuestionResponse> responses = new HashMap<>();

        Cursor cursor = surveyDbAdapter.getResponses(surveyInstanceId);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                QuestionResponseColumns columns = new QuestionResponseColumns(cursor);
                do {
                    QuestionResponse response = getQuestionResponseBuilder(cursor, columns)
                            .setId(null)
                            .setSurveyInstanceId(newSurveyInstanceId)
                            .createQuestionResponse();
                    responses.put(response.responseMapKey(), response);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return responses;
    }

    public QuestionResponse getResponse(Long surveyInstanceId, String questionId) {
        QuestionResponse resp = null;
        Cursor cursor = surveyDbAdapter.getResponse(surveyInstanceId, questionId);
        if (cursor != null && cursor.moveToFirst()) {
            QuestionResponseColumns columns = new QuestionResponseColumns(cursor);
            resp = getQuestionResponseBuilder(cursor, columns)
                    .setQuestionId(questionId)
                    .setSurveyInstanceId(surveyInstanceId)
                    .createQuestionResponse();
        }

        if (cursor != null) {
            cursor.close();
        }

        return resp;
    }

    @NonNull
    private QuestionResponse getResponseToSave(@NonNull QuestionResponse newResponse) {
        QuestionResponse resp;
        Long surveyInstanceId = newResponse.getSurveyInstanceId();
        String questionId = newResponse.getQuestionId();

        Cursor cursor;
        if (newResponse.isAnswerToRepeatableGroup()) {
            cursor = surveyDbAdapter
                    .getResponse(surveyInstanceId, questionId, newResponse.getIteration());
        } else {
            cursor = surveyDbAdapter.getResponse(surveyInstanceId, questionId);
        }
        if (cursor != null && cursor.moveToFirst()) {
            String type = cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.TYPE));
            if (newResponse.getType() != null) {
                type = newResponse.getType();
            }
            Long id = cursor.getLong(cursor.getColumnIndexOrThrow(ResponseColumns._ID));
            int iteration = cursor.getInt(cursor.getColumnIndexOrThrow(ResponseColumns.ITERATION));
            resp = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(newResponse.getValue())
                    .setType(type)
                    .setId(id)
                    .setSurveyInstanceId(surveyInstanceId)
                    .setQuestionId(questionId)
                    .setIncludeFlag(newResponse.getIncludeFlag())
                    .setIteration(iteration)
                    .createQuestionResponse();
        } else {
            resp = new QuestionResponse.QuestionResponseBuilder()
                    .createFromQuestionResponse(newResponse);
        }

        if (cursor != null) {
            cursor.close();
        }
        return resp;
    }


    /**
     * inserts or updates a question response after first looking to see if it
     * already exists in the database.
     *
     * @param newResponse new QuestionResponseData to insert
     */
    public QuestionResponse createOrUpdateSurveyResponse(@NonNull QuestionResponse newResponse) {
        QuestionResponse responseToSave = getResponseToSave(newResponse);
        ContentValues initialValues = new ContentValues();
        initialValues.put(ResponseColumns.ANSWER, responseToSave.getValue());
        initialValues.put(ResponseColumns.TYPE, responseToSave.getType());
        initialValues.put(ResponseColumns.QUESTION_ID, responseToSave.getQuestionId());
        initialValues.put(ResponseColumns.SURVEY_INSTANCE_ID, responseToSave.getSurveyInstanceId());
        initialValues.put(ResponseColumns.INCLUDE, responseToSave.getIncludeFlag() ? 1 : 0);
        initialValues.put(ResponseColumns.ITERATION, responseToSave.getIteration());
        long id = surveyDbAdapter.updateSurveyResponse(responseToSave.getId(), initialValues);
        return new QuestionResponse.QuestionResponseBuilder().createFromQuestionResponse(
                responseToSave, id);
    }

    public long createSurveyRespondent(String surveyId, double version, User user,
            String surveyedLocaleId) {
        final long time = System.currentTimeMillis();

        ContentValues initialValues = new ContentValues();
        initialValues.put(SurveyInstanceColumns.SURVEY_ID, surveyId);
        initialValues.put(SurveyInstanceColumns.VERSION, version);
        initialValues.put(SurveyInstanceColumns.USER_ID, user.getId());
        initialValues.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.SAVED);
        initialValues.put(SurveyInstanceColumns.UUID, PlatformUtil.uuid());
        initialValues.put(SurveyInstanceColumns.START_DATE, time);
        initialValues.put(SurveyInstanceColumns.SAVED_DATE, time);// Default to START_TIME
        initialValues.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
        // Make submitter field available before submission
        initialValues.put(SurveyInstanceColumns.SUBMITTER, user.getName());
        return surveyDbAdapter.createSurveyRespondent(initialValues);
    }

    /**
     * updates a survey in the db and resets the deleted flag to "N"
     *
     */
    public void saveSurvey(Form form) {
        final long surveyGroupId = form.getSurveyId();
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.SURVEY_ID, form.getId());
        updatedValues.put(SurveyColumns.VERSION, form.getVersion());
        updatedValues.put(SurveyColumns.TYPE, form.getType());
        updatedValues.put(SurveyColumns.LOCATION, form.getLocation());
        updatedValues.put(SurveyColumns.FILENAME, form.getFilename());
        updatedValues.put(SurveyColumns.NAME, form.getName());
        updatedValues.put(SurveyColumns.LANGUAGE, form.getLanguage());
        updatedValues.put(SurveyColumns.SURVEY_GROUP_ID, surveyGroupId);
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, form.getCascadeDownloaded() ? 1 : 0);

        briteSurveyDbAdapter.updateSurvey(updatedValues, form.getFormId());
    }

    /**
     * Gets a single survey from the db using its survey id
     */
    public Survey getSurvey(String surveyId) {
        Survey survey = null;
        Cursor cursor = surveyDbAdapter.getSurvey(surveyId);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                survey = surveyMapper.getSurvey(cursor);
            }
            cursor.close();
        }

        return survey;
    }

    public List<FileTransmission> getSurveyInstanceTransmissions(long surveyInstanceId) {
        Cursor cursor = surveyDbAdapter.getSurveyInstanceTransmissions(surveyInstanceId);
        return transmissionsMapper.getFileTransmissions(cursor);
    }

    public void addSurveyGroup(SurveyGroup surveyGroup) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupColumns.SURVEY_GROUP_ID, surveyGroup.getId());
        values.put(SurveyGroupColumns.NAME, surveyGroup.getName());
        values.put(SurveyGroupColumns.REGISTER_SURVEY_ID, surveyGroup.getRegisterSurveyId());
        values.put(SurveyGroupColumns.MONITORED, surveyGroup.isMonitored() ? 1 : 0);
        briteSurveyDbAdapter.addSurveyGroup(values);
    }

    // Attempt to fetch the registration form. If the form ID is explicitly set on the SurveyGroup,
    // we simply query by ID. Otherwise, assume is a non-monitored form, and query the first form
    // we find.
    public Survey getRegistrationForm(SurveyGroup sg) {
        String formId = sg.getRegisterSurveyId();
        if (!TextUtils.isEmpty(formId) && !"null".equalsIgnoreCase(formId)) {
            return getSurvey(formId);
        }
        Survey s = null;
        Cursor c = briteSurveyDbAdapter.getForms(sg.getId());
        if (c != null) {
            if (c.moveToFirst()) {
                s = surveyMapper.getSurvey(c);
            }
            c.close();
        }
        return s;
    }

    private SurveyGroup getSurveyGroup(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyGroupColumns.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.NAME));
        String registerSurveyId = cursor
                .getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.REGISTER_SURVEY_ID));
        boolean monitored =
                cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupColumns.MONITORED)) > 0;
        return new SurveyGroup(id, name, registerSurveyId, monitored);
    }

    public SurveyGroup getSurveyGroup(long id) {
        SurveyGroup sg = null;
        Cursor c = surveyDbAdapter.getSurveyGroup(id);
        if (c != null && c.moveToFirst()) {
            sg = getSurveyGroup(c);
            c.close();
        }

        return sg;
    }

    public void updateSurveyedLocale(long surveyInstanceId, String response,
            SurveyDbAdapter.SurveyedLocaleMeta surveyedLocaleMeta) {
        if (!TextUtils.isEmpty(response)) {
            String surveyedLocaleId = surveyDbAdapter.getSurveyedLocaleId(surveyInstanceId);
            ContentValues surveyedLocaleValues = new ContentValues();

            String questionId = null;
            String type = null;

            switch (surveyedLocaleMeta) {
                case NAME:
                    surveyedLocaleValues.put(RecordColumns.NAME, response);
                    type = "META_NAME";
                    questionId = ConstantUtil.QUESTION_LOCALE_NAME;
                    break;
                case GEOLOCATION:
                    String[] parts = response.split("\\|");
                    if (parts.length < 2) {
                        return;// Wrong format
                    }
                    surveyedLocaleValues.put(RecordColumns.LATITUDE, Double.parseDouble(parts[0]));
                    surveyedLocaleValues.put(RecordColumns.LONGITUDE, Double.parseDouble(parts[1]));
                    type = "META_GEO";
                    questionId = ConstantUtil.QUESTION_LOCALE_GEO;
                    break;
                default:
                    break;
            }
            QuestionResponse metaResponse = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(response)
                    .setType(type)
                    .setSurveyInstanceId(surveyInstanceId)
                    .setQuestionId(questionId)
                    .setIncludeFlag(true)
                    .createQuestionResponse();
            // Update the surveyed locale info
            briteSurveyDbAdapter.updateDataPoint(surveyedLocaleId, surveyedLocaleValues);

            // Store the META_NAME/META_GEO as a response
            createOrUpdateSurveyResponse(metaResponse);
        }
    }

    public void deleteAllSurveys() {
        briteSurveyDbAdapter.deleteAllSurveys();
    }

    public Long getLastSurveyInstance(String mRecordId, String id) {
        return surveyDbAdapter.getLastSurveyInstance(mRecordId, id);
    }

    public void updateSurveyInstanceStatus(long surveyInstanceId, int status) {
        briteSurveyDbAdapter.updateSurveyInstanceStatus(surveyInstanceId, status);
    }

    public void updateRecordModifiedDate(String recordId, long timestamp) {
        briteSurveyDbAdapter.updateRecordModifiedDate(recordId, timestamp);
    }

    public void deleteResponses(String surveyId) {
        surveyDbAdapter.deleteResponses(surveyId);
    }

    public void addSurveyDuration(long mSurveyInstanceId, long timestamp) {
        surveyDbAdapter.addSurveyDuration(mSurveyInstanceId, timestamp);
    }

    public void deleteResponse(long mSurveyInstanceId, String questionId) {
        surveyDbAdapter.deleteResponse(mSurveyInstanceId, questionId);
    }

    public void deleteResponse(long mSurveyInstanceId, String questionId, String iteration) {
        surveyDbAdapter.deleteResponse(mSurveyInstanceId, questionId, iteration);
    }

    public String createSurveyedLocale(long id) {
        return surveyDbAdapter.createSurveyedLocale(id, PlatformUtil.recordUuid());
    }

    public void clearSurveyedLocaleName(long surveyInstanceId) {
        surveyDbAdapter.clearSurveyedLocaleName(surveyInstanceId);
    }

    public void clearCollectedData() {
        surveyDbAdapter.clearCollectedData();
    }

    public long createOrUpdateUser(Long id, String username) {
        return briteSurveyDbAdapter.createOrUpdateUser(id, username);
    }

    public void deleteAllResponses() {
        surveyDbAdapter.deleteAllResponses();
    }

    public void addQuestionGroups(Form form) {
        briteSurveyDbAdapter.deleteQuestionGroups(form.getFormId());
        List<QuestionGroup> groups = form.getGroups();
        List<ContentValues> groupValues = new ArrayList<>();
        for (QuestionGroup group: groups) {
            ContentValues values = new ContentValues();
            Long groupId = group.getGroupId();
            if (groupId == null) {
                groupId = -1L;
            }
            values.put(COLUMN_GROUP_ID, groupId);
            values.put(COLUMN_HEADING, group.getHeading());
            values.put(COLUMN_REPEATABLE, group.getRepeatable() ? 1 : 0);
            values.put(COLUMN_FORM_ID, group.getFormId());
            values.put(COLUMN_ORDER, group.getOrder());
            groupValues.add(values);
        }
        briteSurveyDbAdapter.saveQuestionGroup(groupValues, form.getFormId());
    }
}
