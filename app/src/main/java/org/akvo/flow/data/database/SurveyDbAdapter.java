/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static org.akvo.flow.data.database.SurveyInstanceStatus.SAVED;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 *
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {

    private static final String SURVEY_INSTANCE_JOIN_RESPONSE_USER = "survey_instance "
            + "LEFT OUTER JOIN response ON survey_instance._id=response.survey_instance_id "
            + "LEFT OUTER JOIN user ON survey_instance.user_id=user._id";
    private static final String SURVEY_INSTANCE_JOIN_SURVEY = "survey_instance "
            + "JOIN survey ON survey_instance.survey_id = survey.survey_id "
            + "JOIN survey_group ON survey.survey_group_id=survey_group.survey_group_id";
    private static final String SURVEY_INSTANCE_JOIN_SURVEY_AND_RESPONSE = "survey_instance "
            + "JOIN survey ON survey_instance.survey_id = survey.survey_id "
            + "JOIN survey_group ON survey.survey_group_id=survey_group.survey_group_id "
            + "JOIN response ON survey_instance._id=response.survey_instance_id";

    public static final String SURVEY_JOIN_SURVEY_INSTANCE =
            "survey LEFT OUTER JOIN survey_instance ON "
            + "survey.survey_id=survey_instance.survey_id";

    private static final int DOES_NOT_EXIST = -1;

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private final Context context;

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public SurveyDbAdapter(Context ctx) {
        this.context = ctx;
    }

    /**
     * Open or create the db
     *
     * @throws SQLException if the database could be neither opened or created
     */
    public SurveyDbAdapter open() throws SQLException {
        databaseHelper = new DatabaseHelper(context, new LanguageTable());
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db
     */
    public void close() {
        databaseHelper.close();
    }

    public Cursor getSurveyInstancesByStatus(int status) {
        return database.query(Tables.SURVEY_INSTANCE,
                new String[] { SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID },
                SurveyInstanceColumns.STATUS + " = ?",
                new String[] { String.valueOf(status) },
                null, null, null);
    }

    public Cursor getResponsesData(long surveyInstanceId) {
        return database.query(SURVEY_INSTANCE_JOIN_RESPONSE_USER,
                new String[] {
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.START_DATE,
                        SurveyInstanceColumns.RECORD_ID, SurveyInstanceColumns.DURATION,
                        ResponseColumns.ANSWER, ResponseColumns.TYPE, ResponseColumns.QUESTION_ID,
                        ResponseColumns.FILENAME, UserColumns.NAME, UserColumns.EMAIL
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.INCLUDE + " = 1",
                new String[] {
                        String.valueOf(surveyInstanceId)
                }, null, null, null);
    }

    /**
     * updates the status of a survey instance to the status passed in.
     * Status must be one of the 'SurveyInstanceStatus' one. The corresponding
     * Date column will be updated with the current timestamp.
     *
     * @param surveyInstanceId
     * @param status
     */
    public void updateSurveyStatus(long surveyInstanceId, int status) {
        String dateColumn;
        switch (status) {
            case SurveyInstanceStatus.DOWNLOADED:
            case SurveyInstanceStatus.SYNCED:
                dateColumn = SurveyInstanceColumns.SYNC_DATE;
                break;
            case SurveyInstanceStatus.EXPORTED:
                dateColumn = SurveyInstanceColumns.EXPORTED_DATE;
                break;
            case SurveyInstanceStatus.SUBMITTED:
                dateColumn = SurveyInstanceColumns.SUBMITTED_DATE;
                break;
            case SAVED:
                dateColumn = SurveyInstanceColumns.SAVED_DATE;
                break;
            default:
                return;// Nothing to see here, buddy
        }

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyInstanceColumns.STATUS, status);
        updatedValues.put(dateColumn, System.currentTimeMillis());

        final int rows = database.update(Tables.SURVEY_INSTANCE,
                updatedValues,
                SurveyInstanceColumns._ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) });

        if (rows < 1) {
            Timber.e("Could not update status for Survey Instance: " + surveyInstanceId);
        }
    }

    /**
     * Increment the duration of a particular respondent.
     * The provided value will be added on top of the already stored one (default to 0).
     * This will allow users to pause and resume a survey without considering that
     * time as part of the survey duration.
     *
     * @param sessionDuration time spent in the current session
     */
    public void addSurveyDuration(long respondentId, long sessionDuration) {
        final String sql = "UPDATE " + Tables.SURVEY_INSTANCE
                + " SET " + SurveyInstanceColumns.DURATION + " = "
                + SurveyInstanceColumns.DURATION + " + " + sessionDuration
                + " WHERE " + SurveyInstanceColumns._ID + " = " + respondentId
                + " AND " + SurveyInstanceColumns.SUBMITTED_DATE + " IS NULL";
        database.execSQL(sql);
    }

    /**
     * returns a cursor listing all users
     *
     * @return
     */
    public Cursor getUsers() {
        Cursor cursor = database.query(Tables.USER,
                new String[] { UserColumns._ID, UserColumns.NAME, UserColumns.EMAIL },
                UserColumns.DELETED + " <> ?",
                new String[] { "1" },
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * retrieves a user by ID
     *
     * @param id
     * @return
     */
    public Cursor getUser(Long id) {
        Cursor cursor = database.query(Tables.USER,
                new String[] { UserColumns._ID, UserColumns.NAME, UserColumns.EMAIL },
                UserColumns._ID + "=?",
                new String[] { id.toString() },
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * if the ID is populated, this will update a user record. Otherwise, it
     * will be inserted
     */
    public long createOrUpdateUser(Long id, String name) {
        ContentValues initialValues = new ContentValues();
        Long idVal = id;
        initialValues.put(UserColumns.NAME, name);
        initialValues.put(UserColumns.DELETED, 0);

        if (idVal == null) {
            idVal = database.insert(Tables.USER, null, initialValues);
        } else {
            database.update(Tables.USER, initialValues, UserColumns._ID + "=?",
                    new String[] { idVal.toString() });
        }
        return idVal;
    }

    public Map<String, QuestionResponse> getResponses(long surveyInstanceId) {
        Map<String, QuestionResponse> responses = new HashMap<>();

        Cursor cursor = database.query(Tables.RESPONSE,
                new String[] {
                        ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                        ResponseColumns.INCLUDE, ResponseColumns.FILENAME
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(ResponseColumns._ID);
            int answerCol = cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            int typeCol = cursor.getColumnIndexOrThrow(ResponseColumns.TYPE);
            int qidCol = cursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
            int includeCol = cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE);
            int filenameCol = cursor.getColumnIndexOrThrow(ResponseColumns.FILENAME);

            if (cursor.moveToFirst()) {
                do {
                    QuestionResponse response = new QuestionResponse();
                    response.setId(cursor.getLong(idCol));
                    response.setRespondentId(surveyInstanceId);// No need to read the cursor
                    response.setValue(cursor.getString(answerCol));
                    response.setType(cursor.getString(typeCol));
                    response.setQuestionId(cursor.getString(qidCol));
                    response.setIncludeFlag(cursor.getInt(includeCol) == 1);
                    response.setFilename(cursor.getString(filenameCol));

                    responses.put(response.getQuestionId(), response);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return responses;
    }

    /**
     * loads a single question response
     *
     * @param surveyInstanceId
     * @param questionId
     * @return
     */
    public QuestionResponse getResponse(Long surveyInstanceId, String questionId) {
        QuestionResponse resp = null;
        Cursor cursor = database.query(Tables.RESPONSE,
                new String[] {
                        ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                        ResponseColumns.INCLUDE, ResponseColumns.FILENAME
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                        + " =?",
                new String[] { String.valueOf(surveyInstanceId), questionId },
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            resp = new QuestionResponse();
            resp.setQuestionId(questionId);
            resp.setRespondentId(surveyInstanceId);
            resp.setType(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.TYPE)));
            resp.setValue(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER)));
            resp.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ResponseColumns._ID)));
            resp.setFilename(
                    cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.FILENAME)));

            boolean include =
                    cursor.getInt(cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE)) == 1;
            resp.setIncludeFlag(include);
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
     * @param resp
     * @return
     */
    public QuestionResponse createOrUpdateSurveyResponse(QuestionResponse resp) {
        QuestionResponse responseToSave = getResponse(
                resp.getRespondentId(), resp.getQuestionId());
        if (responseToSave != null) {
            responseToSave.setValue(resp.getValue());
            responseToSave.setFilename(resp.getFilename());
            if (resp.getType() != null) {
                responseToSave.setType(resp.getType());
            }
        } else {
            responseToSave = resp;
        }
        long id = -1;
        ContentValues initialValues = new ContentValues();
        initialValues.put(ResponseColumns.ANSWER, responseToSave.getValue());
        initialValues.put(ResponseColumns.TYPE, responseToSave.getType());
        initialValues.put(ResponseColumns.QUESTION_ID, responseToSave.getQuestionId());
        initialValues.put(ResponseColumns.SURVEY_INSTANCE_ID, responseToSave.getRespondentId());
        initialValues.put(ResponseColumns.FILENAME, responseToSave.getFilename());
        initialValues.put(ResponseColumns.INCLUDE, resp.getIncludeFlag() ? 1 : 0);
        if (responseToSave.getId() == null) {
            id = database.insert(Tables.RESPONSE, null, initialValues);
        } else {
            if (database.update(Tables.RESPONSE, initialValues, ResponseColumns._ID
                    + "=?", new String[] {
                    responseToSave.getId().toString()
            }) > 0) {
                id = responseToSave.getId();
            }
        }
        responseToSave.setId(id);
        resp.setId(id);
        return responseToSave;
    }

    /**
     * creates a new unsubmitted survey instance
     */
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
        return database.insert(Tables.SURVEY_INSTANCE, null, initialValues);
    }

    /**
     * returns a list of survey objects that are out of date (missing from the
     * db or with a lower version number). If a survey is present but marked as
     * deleted, it will not be listed as out of date (and thus won't be updated)
     *
     * @param surveys
     * @return
     */
    public List<Survey> checkSurveyVersions(List<Survey> surveys) {
        List<Survey> outOfDateSurveys = new ArrayList<>();
        for (int i = 0; i < surveys.size(); i++) {
            Cursor cursor = database.query(Tables.SURVEY,
                    new String[] {
                            SurveyColumns.SURVEY_ID
                    },
                    SurveyColumns.SURVEY_ID + " = ? and (" + SurveyColumns.VERSION + " >= ? or "
                            + SurveyColumns.DELETED + " = ?)", new String[] {
                            surveys.get(i).getId(),
                            surveys.get(i).getVersion() + "",
                            String.valueOf(1)//ConstantUtil.IS_DELETED
                    }, null, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                outOfDateSurveys.add(surveys.get(i));
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return outOfDateSurveys;
    }

    /**
     * updates the survey table by recording the help download flag
     */
    public void markSurveyHelpDownloaded(String surveyId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, isDownloaded ? 1 : 0);

        if (database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        surveyId
                }) < 1) {
            Timber.e("Could not update record for Survey " + surveyId);
        }
    }

    /**
     * updates a survey in the db and resets the deleted flag to "N"
     *
     * @param survey
     * @return
     */
    public void saveSurvey(Survey survey) {
        Cursor cursor = database.query(Tables.SURVEY,
                new String[] {
                        SurveyColumns._ID
                }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        survey.getId(),
                }, null, null, null);
        final long surveyGroupId = survey.getSurveyGroup() != null ?
                survey.getSurveyGroup().getId()
                : SurveyGroup.ID_NONE;

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.SURVEY_ID, survey.getId());
        updatedValues.put(SurveyColumns.VERSION, survey.getVersion());
        updatedValues.put(SurveyColumns.TYPE, survey.getType());
        updatedValues.put(SurveyColumns.LOCATION, survey.getLocation());
        updatedValues.put(SurveyColumns.FILENAME, survey.getFileName());
        updatedValues.put(SurveyColumns.NAME, survey.getName());
        updatedValues.put(SurveyColumns.LANGUAGE, survey.getDefaultLanguageCode());
        updatedValues.put(SurveyColumns.SURVEY_GROUP_ID, surveyGroupId);
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, survey.isHelpDownloaded() ? 1 : 0);
        //updatedValues.put(SurveyColumns., ConstantUtil.NOT_DELETED);

        if (cursor != null && cursor.getCount() > 0) {
            // if we found an item, it's an update, otherwise, it's an insert
            database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                    new String[] {
                            survey.getId()
                    });
        } else {
            database.insert(Tables.SURVEY, null, updatedValues);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Gets a single survey from the db using its survey id
     */
    public Survey getSurvey(String surveyId) {
        Survey survey = null;
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                        SurveyColumns.SURVEY_ID, SurveyColumns.NAME, SurveyColumns.LOCATION,
                        SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                        SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION
                }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        surveyId
                }, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                survey = getSurvey(cursor);
            }
            cursor.close();
        }

        return survey;
    }

    /**
     * deletes all the surveys from the database
     */
    public void deleteAllSurveys() {
        database.delete(Tables.SURVEY, null, null);
        database.delete(Tables.SURVEY_GROUP, null, null);
    }

    /**
     * deletes all survey responses from the database for a specific survey instance
     */
    public void deleteResponses(String surveyInstanceId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ?",
                new String[] {
                        surveyInstanceId
                });
    }

    /**
     * deletes the surveyInstanceId record and any responses it contains
     *
     * @param surveyInstanceId
     */
    public void deleteSurveyInstance(String surveyInstanceId) {
        deleteResponses(surveyInstanceId);
        database.delete(Tables.SURVEY_INSTANCE, SurveyInstanceColumns._ID + "=?",
                new String[] {
                        surveyInstanceId
                });
    }

    /**
     * deletes a single response
     *
     * @param surveyInstanceId
     * @param questionId
     */
    public void deleteResponse(long surveyInstanceId, String questionId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ? AND "
                + ResponseColumns.QUESTION_ID + "= ?", new String[] {
                String.valueOf(surveyInstanceId),
                questionId
        });
    }

    public void createTransmission(long surveyInstanceId, String formID, String filename) {
        createTransmission(surveyInstanceId, formID, filename, TransmissionStatus.QUEUED);
    }

    public void createTransmission(long surveyInstanceId, String formID, String filename,
            int status) {
        ContentValues values = new ContentValues();
        values.put(TransmissionColumns.SURVEY_INSTANCE_ID, surveyInstanceId);
        values.put(TransmissionColumns.SURVEY_ID, formID);
        values.put(TransmissionColumns.FILENAME, filename);
        values.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            final String date = String.valueOf(System.currentTimeMillis());
            values.put(TransmissionColumns.START_DATE, date);
            values.put(TransmissionColumns.END_DATE, date);
        }
        database.insert(Tables.TRANSMISSION, null, values);
    }

    /**
     * Updates the matching transmission history records with the status
     * passed in. If the status == Completed, the completion date is updated. If
     * the status == In Progress, the start date is updated.
     *
     * @param fileName
     * @param status
     * @return the number of rows affected
     */
    public int updateTransmissionHistory(String fileName, int status) {
        // TODO: Update Survey Instance STATUS as well
        ContentValues vals = new ContentValues();
        vals.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            vals.put(TransmissionColumns.END_DATE, System.currentTimeMillis() + "");
        } else if (TransmissionStatus.IN_PROGRESS == status) {
            vals.put(TransmissionColumns.START_DATE, System.currentTimeMillis() + "");
        }

        return database.update(Tables.TRANSMISSION, vals,
                TransmissionColumns.FILENAME + " = ?",
                new String[] { fileName });
    }

    private List<FileTransmission> getFileTransmissions(Cursor cursor) {
        List<FileTransmission> transmissions = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int startCol = cursor.getColumnIndexOrThrow(TransmissionColumns.START_DATE);
                final int endCol = cursor.getColumnIndexOrThrow(TransmissionColumns.END_DATE);
                final int idCol = cursor.getColumnIndexOrThrow(TransmissionColumns._ID);
                final int formIdCol = cursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_ID);
                final int surveyInstanceCol = cursor
                        .getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID);
                final int fileCol = cursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME);
                final int statusCol = cursor.getColumnIndexOrThrow(TransmissionColumns.STATUS);

                transmissions = new ArrayList<>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(idCol));
                    trans.setFormId(cursor.getString(formIdCol));
                    trans.setRespondentId(cursor.getLong(surveyInstanceCol));
                    trans.setFileName(cursor.getString(fileCol));
                    trans.setStatus(cursor.getInt(statusCol));

                    // Start and End date. Handle null cases
                    if (!cursor.isNull(startCol)) {
                        trans.setStartDate(new Date(cursor.getLong(startCol)));
                    }
                    if (!cursor.isNull(endCol)) {
                        trans.setEndDate(new Date(cursor.getLong(endCol)));
                    }

                    transmissions.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return transmissions;
    }

    public List<FileTransmission> getFileTransmissions(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.SURVEY_ID, TransmissionColumns.STATUS,
                        TransmissionColumns.FILENAME, TransmissionColumns.START_DATE,
                        TransmissionColumns.END_DATE
                },
                TransmissionColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        return getFileTransmissions(cursor);
    }

    /**
     * Get the list of queued and failed transmissions
     */
    public List<FileTransmission> getUnsyncedTransmissions() {
        Cursor cursor = database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.SURVEY_ID, TransmissionColumns.STATUS,
                        TransmissionColumns.FILENAME, TransmissionColumns.START_DATE,
                        TransmissionColumns.END_DATE
                },
                TransmissionColumns.STATUS + " IN (?, ?, ?)",
                new String[] {
                        String.valueOf(TransmissionStatus.FAILED),
                        String.valueOf(TransmissionStatus.IN_PROGRESS),// Stalled IN_PROGRESS files
                        String.valueOf(TransmissionStatus.QUEUED)
                }, null, null, null);

        return getFileTransmissions(cursor);
    }

    /**
     * executes a single insert/update/delete DML or any DDL statement without
     * any bind arguments.
     *
     * @param sql
     */
    public void executeSql(String sql) {
        database.execSQL(sql);
    }

    /**
     * reinserts the test survey into the database. For debugging purposes only.
     * The survey xml must exist in the APK
     */
    public void reinstallTestSurvey() {
        ContentValues values = new ContentValues();
        values.put(SurveyColumns.SURVEY_ID, "999991");
        values.put(SurveyColumns.NAME, "Sample Survey");
        values.put(SurveyColumns.VERSION, 1.0);
        values.put(SurveyColumns.TYPE, "Survey");
        values.put(SurveyColumns.LOCATION, "res");
        values.put(SurveyColumns.FILENAME, "999991.xml");
        values.put(SurveyColumns.LANGUAGE, "en");
        database.insert(Tables.SURVEY, null, values);
    }

    /**
     * permanently deletes all surveys, responses, users and transmission
     * history from the database
     */
    public void clearAllData() {
        // User generated data
        clearCollectedData();

        // Surveys and preferences
        executeSql("DELETE FROM " + Tables.SURVEY);
        executeSql("DELETE FROM " + Tables.SURVEY_GROUP);
        executeSql("DELETE FROM " + Tables.USER);
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        executeSql("DELETE FROM " + Tables.SYNC_TIME);
        executeSql("DELETE FROM " + Tables.RESPONSE);
        executeSql("DELETE FROM " + Tables.SURVEY_INSTANCE);
        executeSql("DELETE FROM " + Tables.RECORD);
        executeSql("DELETE FROM " + Tables.TRANSMISSION);
    }

    /**
     * performs a soft-delete on a user
     *
     * @param id
     */
    public void deleteUser(Long id) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(UserColumns.DELETED, 1);
        database.update(Tables.USER, updatedValues, UserColumns._ID + " = ?",
                new String[] {
                        id.toString()
                });
    }

    public void addSurveyGroup(SurveyGroup surveyGroup) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupColumns.SURVEY_GROUP_ID, surveyGroup.getId());
        values.put(SurveyGroupColumns.NAME, surveyGroup.getName());
        values.put(SurveyGroupColumns.REGISTER_SURVEY_ID, surveyGroup.getRegisterSurveyId());
        values.put(SurveyGroupColumns.MONITORED, surveyGroup.isMonitored() ? 1 : 0);
        database.insert(Tables.SURVEY_GROUP, null, values);
    }

    public static SurveyGroup getSurveyGroup(Cursor cursor) {
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
        Cursor c = database.query(Tables.SURVEY_GROUP,
                new String[] {
                        SurveyGroupColumns._ID, SurveyGroupColumns.SURVEY_GROUP_ID,
                        SurveyGroupColumns.NAME,
                        SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED
                },
                SurveyGroupColumns.SURVEY_GROUP_ID + "= ?",
                new String[] { String.valueOf(id) },
                null, null, null);
        if (c != null && c.moveToFirst()) {
            sg = getSurveyGroup(c);
            c.close();
        }

        return sg;
    }

    public Cursor getSurveyGroups() {
        return database.query(Tables.SURVEY_GROUP,
                new String[] {
                        SurveyGroupColumns._ID, SurveyGroupColumns.SURVEY_GROUP_ID,
                        SurveyGroupColumns.NAME,
                        SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED
                },
                null, null, null, null, SurveyGroupColumns.NAME);
    }

    public String createSurveyedLocale(long surveyGroupId) {
        String id = PlatformUtil.recordUuid();
        ContentValues values = new ContentValues();
        values.put(RecordColumns.RECORD_ID, id);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        database.insert(Tables.RECORD, null, values);

        return id;
    }

    public static SurveyedLocale getSurveyedLocale(Cursor cursor) {
        String id = cursor.getString(RecordQuery.RECORD_ID);
        long surveyGroupId = cursor.getLong(RecordQuery.SURVEY_GROUP_ID);
        long lastModified = cursor.getLong(RecordQuery.LAST_MODIFIED);
        String name = cursor.getString(RecordQuery.NAME);

        // Location. Check for null values first
        Double latitude = null;
        Double longitude = null;
        if (!cursor.isNull(RecordQuery.LATITUDE) && !cursor.isNull(RecordQuery.LONGITUDE)) {
            latitude = cursor.getDouble(RecordQuery.LATITUDE);
            longitude = cursor.getDouble(RecordQuery.LONGITUDE);
        }
        return new SurveyedLocale(id, name, lastModified, surveyGroupId, latitude, longitude);
    }

    public Cursor getSurveyedLocales(long surveyGroupId) {
        return database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.SURVEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(surveyGroupId) },
                null, null, null);
    }

    @Nullable
    public SurveyedLocale getSurveyedLocale(String surveyedLocaleId) {
        Cursor cursor = database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.RECORD_ID + " = ?",
                new String[] {surveyedLocaleId},
                null, null, null);

        SurveyedLocale locale = null;
        if (cursor.moveToFirst()) {
            locale = getSurveyedLocale(cursor);
        }
        cursor.close();

        return locale;
    }

    private static Survey getSurvey(Cursor cursor) {
        Survey survey = new Survey();
        survey.setId(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID)));
        survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME)));
        survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION)));
        survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME)));
        survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE)));
        survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
        survey.setVersion(cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION)));

        int helpDownloaded = cursor
                .getInt(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED));
        survey.setHelpDownloaded(helpDownloaded == 1);
        return survey;
    }

    public String[] getSurveyIds() {
        Cursor c = getSurveys(-1);// All survey groups
        if (c != null) {
            String[] ids = new String[c.getCount()];
            if (c.moveToFirst()) {
                do {
                    ids[c.getPosition()] = c
                            .getString(c.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID));
                } while (c.moveToNext());
            }
            c.close();
            return ids;
        }
        return null;
    }

    // Attempt to fetch the registation form. If the form ID is explicitely set on the SurveyGroup,
    // we simply query by ID. Otherwise, assume is a non-monitored form, and query the first form
    // we find.
    public Survey getRegistrationForm(SurveyGroup sg) {
        String formId = sg.getRegisterSurveyId();
        if (!TextUtils.isEmpty(formId) && !"null".equalsIgnoreCase(formId)) {
            return getSurvey(formId);
        }
        Survey s = null;
        Cursor c = getSurveys(sg.getId());
        if (c != null) {
            if (c.moveToFirst()) {
                s = getSurvey(c);
            }
            c.close();
        }
        return s;
    }

    private Cursor getSurveys(long surveyGroupId) {
        String whereClause = SurveyColumns.DELETED + " <> 1";
        String[] whereParams = null;
        if (surveyGroupId > 0) {
            whereClause += " AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[] {
                    String.valueOf(surveyGroupId)
            };
        }

        return database.query(Tables.SURVEY, new String[] {
                        SurveyColumns._ID, SurveyColumns.SURVEY_ID, SurveyColumns.NAME,
                        SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                        SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION, SurveyColumns.LOCATION
                },
                whereClause, whereParams, null, null, null);
    }

    /**
     * Convenience method to retrieve all non-deleted surveys, without the hassle of
     * parsing the Cursor columns.
     * To get the Cursor result, use getSurveys(surveyGroupId)
     */
    public List<Survey> getSurveyList(long surveyGroupId) {
        // Reuse getSurveys() method
        Cursor cursor = getSurveys(surveyGroupId);

        ArrayList<Survey> surveys = new ArrayList<>();

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    surveys.add(getSurvey(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveys;
    }

    public void deleteSurveyGroup(long surveyGroupId) {
        // First the group
        database.delete(Tables.SURVEY_GROUP, SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
        // Now the surveys
        database.delete(Tables.SURVEY, SurveyColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
    }

    /**
     * marks a survey record identified by the ID passed in as deleted.
     *
     * @param surveyId
     */
    public void deleteSurvey(String surveyId) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.DELETED, 1);
        database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                new String[] { surveyId });
    }

    public Cursor getFormInstance(long formInstanceId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID + "= ?",
                new String[] { String.valueOf(formInstanceId) },
                null, null, null);
    }

    /**
     * Get all the SurveyInstances for a particular data point. Registration form will be at the top
     * of the list, all other forms will be ordered by submission date (desc).
     */
    public Cursor getFormInstances(String recordId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { recordId },
                null, null,
                "CASE WHEN survey.survey_id = survey_group.register_survey_id THEN 0 ELSE 1 END, "
                        + SurveyInstanceColumns.START_DATE + " DESC");
    }

    /**
     * Get all the SurveyInstances for a particular data point which actually have non empty
     * responses. Registration form will be at the top of the list, all other forms will be ordered
     * by submission date (desc).
     */
    public Cursor getFormInstancesWithResponses(String recordId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY_AND_RESPONSE,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { recordId },
                ResponseColumns.SURVEY_INSTANCE_ID, null,
                "CASE WHEN survey.survey_id = survey_group.register_survey_id THEN 0 ELSE 1 END, "
                        + SurveyInstanceColumns.START_DATE + " DESC");
    }

    /**
     * Get SurveyInstances with a particular status.
     * If the recordId is not null, results will be filtered by Record.
     */
    public long[] getFormInstances(String recordId, String surveyId, int status) {
        String where = Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND " + SurveyInstanceColumns.STATUS + "= ?" +
                " AND " + SurveyInstanceColumns.RECORD_ID + "= ?";
        List<String> args = new ArrayList<>();
        args.add(surveyId);
        args.add(String.valueOf(status));
        args.add(recordId);

        Cursor c = database.query(Tables.SURVEY_INSTANCE,
                new String[] { SurveyInstanceColumns._ID },
                where, args.toArray(new String[args.size()]),
                null, null, SurveyInstanceColumns.START_DATE + " DESC");

        long[] instances = new long[0];// Avoid null array
        if (c != null) {
            instances = new long[c.getCount()];
            if (c.moveToFirst()) {
                do {
                    instances[c.getPosition()] = c.getLong(0);// Single column (ID)
                } while (c.moveToNext());
            }
            c.close();
        }
        return instances;
    }

    /**
     * Given a particular surveyedLocale and one of its surveys,
     * retrieves the ID of the last surveyInstance matching that criteria
     *
     * @param surveyedLocaleId
     * @param surveyId
     * @return last surveyInstance with those attributes
     */
    public Long getLastSurveyInstance(String surveyedLocaleId, String surveyId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID,
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE
                },
                SurveyInstanceColumns.RECORD_ID + "= ? AND " + SurveyInstanceColumns.SURVEY_ID
                        + "= ? AND " + SurveyInstanceColumns.SUBMITTED_DATE + " IS NOT NULL",
                new String[] { surveyedLocaleId, surveyId },
                null, null,
                SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            long surveyInstanceId = cursor
                    .getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
            cursor.close();
            return surveyInstanceId;
        }

        return null;
    }

    private String getSurveyedLocaleId(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID
                },
                SurveyInstanceColumns._ID + "= ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        String id = null;
        if (cursor.moveToFirst()) {
            id = cursor.getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.RECORD_ID));
        }
        cursor.close();
        return id;
    }

    public void clearSurveyedLocaleName(long surveyInstanceId) {
        String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
        ContentValues surveyedLocaleValues = new ContentValues();
        surveyedLocaleValues.put(RecordColumns.NAME, "");
        database.update(Tables.RECORD, surveyedLocaleValues,
                RecordColumns.RECORD_ID + " = ?",
                new String[] { surveyedLocaleId });
    }

    public Cursor getDatapointStatus(String recordId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns.STATUS
                },
                SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { String.valueOf(recordId) },
                null, null, null);
        return cursor;
    }

    /**
     * Flag to indicate the type of locale update from a given response
     */
    public enum SurveyedLocaleMeta {
        NAME, GEOLOCATION
    }

    public void updateSurveyedLocale(long surveyInstanceId, String response,
            SurveyedLocaleMeta type) {
        if (!TextUtils.isEmpty(response)) {
            String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
            ContentValues surveyedLocaleValues = new ContentValues();

            QuestionResponse metaResponse = new QuestionResponse();
            metaResponse.setRespondentId(surveyInstanceId);
            metaResponse.setValue(response);
            metaResponse.setIncludeFlag(true);

            switch (type) {
                case NAME:
                    surveyedLocaleValues.put(RecordColumns.NAME, response);
                    metaResponse.setType("META_NAME");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_NAME);
                    break;
                case GEOLOCATION:
                    String[] parts = response.split("\\|");
                    if (parts.length < 2) {
                        return;// Wrong format
                    }
                    surveyedLocaleValues.put(RecordColumns.LATITUDE, Double.parseDouble(parts[0]));
                    surveyedLocaleValues.put(RecordColumns.LONGITUDE, Double.parseDouble(parts[1]));
                    metaResponse.setType("META_GEO");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_GEO);
                    break;
            }

            // Update the surveyed locale info
            database.update(Tables.RECORD, surveyedLocaleValues,
                    RecordColumns.RECORD_ID + " = ?",
                    new String[] { surveyedLocaleId });

            // Store the META_NAME/META_GEO as a response
            createOrUpdateSurveyResponse(metaResponse);
        }
    }

    /**
     * Update the last modification date, if necessary
     */
    public void updateRecordModifiedDate(String recordId, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.LAST_MODIFIED, timestamp);
        database.update(Tables.RECORD, values,
                RecordColumns.RECORD_ID + " = ? AND " + RecordColumns.LAST_MODIFIED + " < ?",
                new String[] { recordId, String.valueOf(timestamp) });
    }

    /**
     * Filters surveyd locales based on the parameters passed in.
     */
    public Cursor getFilteredSurveyedLocales(long surveyGroupId, Double latitude, Double longitude,
            int orderBy) {
        // Note: This PROJECTION column indexes have to match the default RecordQuery PROJECTION ones,
        // as this one will only APPEND new columns to the resultset, making the generic getSurveyedLocale(Cursor)
        // fully compatible. TODO: This should be refactored and replaced with a less complex approach.
        String queryString = "SELECT sl.*,"
                + " MIN(r." + SurveyInstanceColumns.STATUS + ") as " + SurveyInstanceColumns.STATUS
                + " FROM "
                + Tables.RECORD + " AS sl LEFT JOIN " + Tables.SURVEY_INSTANCE + " AS r ON "
                + "sl." + RecordColumns.RECORD_ID + "=" + "r." + SurveyInstanceColumns.RECORD_ID;
        String whereClause = " WHERE sl." + RecordColumns.SURVEY_GROUP_ID + " =?";
        String groupBy = " GROUP BY sl." + RecordColumns.RECORD_ID;

        String orderByStr = "";
        switch (orderBy) {
            case ConstantUtil.ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";// By date
                break;
            case ConstantUtil.ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null) {
                    // this is to correct the distance for the shortening at higher latitudes
                    Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)), 2);

                    // this uses a simple planar approximation of distance. this should be good enough for our purpose.
                    String orderByTempl = " ORDER BY CASE WHEN " + RecordColumns.LATITUDE
                            + " IS NULL THEN 1 ELSE 0 END,"
                            + " ((%s - " + RecordColumns.LATITUDE + ") * (%s - "
                            + RecordColumns.LATITUDE
                            + ") + (%s - " + RecordColumns.LONGITUDE + ") * (%s - "
                            + RecordColumns.LONGITUDE + ") * %s)";
                    orderByStr = String
                            .format(orderByTempl, latitude, latitude, longitude, longitude, fudge);
                }
                break;
            case ConstantUtil.ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case ConstantUtil.ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " COLLATE NOCASE ASC";// By name
                break;
        }

        String[] whereValues = new String[] { String.valueOf(surveyGroupId) };
        return database.rawQuery(queryString + whereClause + groupBy + orderByStr, whereValues);
    }

    // ======================================================= //
    // =========== SurveyedLocales synchronization =========== //
    // ======================================================= //

    private void syncResponses(List<QuestionResponse> responses, long surveyInstanceId) {
        for (QuestionResponse response : responses) {
            Cursor cursor = database.query(Tables.RESPONSE,
                    new String[] { ResponseColumns.SURVEY_INSTANCE_ID, ResponseColumns.QUESTION_ID
                    },
                    ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                            + ResponseColumns.QUESTION_ID + " = ?",
                    new String[] { String.valueOf(surveyInstanceId), response.getQuestionId() },
                    null, null, null);

            boolean exists = cursor.getCount() > 0;
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(ResponseColumns.ANSWER, response.getValue());
            values.put(ResponseColumns.TYPE, response.getType());
            values.put(ResponseColumns.QUESTION_ID, response.getQuestionId());
            values.put(ResponseColumns.INCLUDE, response.getIncludeFlag());
            values.put(ResponseColumns.SURVEY_INSTANCE_ID, surveyInstanceId);

            if (exists) {
                database.update(Tables.RESPONSE, values,
                        ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                                + ResponseColumns.QUESTION_ID + " = ?",
                        new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()
                        });
            } else {
                database.insert(Tables.RESPONSE, null, values);
            }
        }
    }

    private void syncSurveyInstances(List<SurveyInstance> surveyInstances,
            String surveyedLocaleId) {
        for (SurveyInstance surveyInstance : surveyInstances) {
            Cursor cursor = database.query(Tables.SURVEY_INSTANCE, new String[] {
                            SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID
                    },
                    SurveyInstanceColumns.UUID + " = ?",
                    new String[] { surveyInstance.getUuid() },
                    null, null, null);

            long surveyInstanceId = DOES_NOT_EXIST;
            if (cursor.moveToFirst()) {
                surveyInstanceId = cursor.getLong(0);
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getDate());
            values.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
            values.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
            values.put(SurveyInstanceColumns.SUBMITTER, surveyInstance.getSubmitter());

            if (surveyInstanceId != DOES_NOT_EXIST) {
                database.update(Tables.SURVEY_INSTANCE, values, SurveyInstanceColumns.UUID
                        + " = ?", new String[] { surveyInstance.getUuid() });
            } else {
                values.put(SurveyInstanceColumns.UUID, surveyInstance.getUuid());
                surveyInstanceId = database.insert(Tables.SURVEY_INSTANCE, null, values);
            }

            syncResponses(surveyInstance.getResponses(), surveyInstanceId);
            updateTransmission(surveyInstance, surveyInstanceId);
        }
    }

    private void updateTransmission(SurveyInstance surveyInstance, long surveyInstanceId) {
        // The filename is a unique column in the transmission table, if we do not have
        // a file to hold this data, we set the value to the instance UUID
        Cursor cursor = null;
        if (surveyInstanceId != DOES_NOT_EXIST) {
            cursor = database.query(Tables.TRANSMISSION,
                    new String[] {
                            TransmissionColumns._ID,
                    },
                    TransmissionColumns.SURVEY_INSTANCE_ID + " = ? ",
                    new String[] { String.valueOf(surveyInstanceId)},
                    null, null, null);
        }
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(TransmissionColumns._ID);
            do {
                updateTransmission(cursor.getInt(columnIndex));
            } while (cursor.moveToNext());
        } else {
            createTransmission(surveyInstanceId, surveyInstance.getSurveyId(),
                    surveyInstance.getUuid(),
                    TransmissionStatus.SYNCED);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void updateTransmission(int transmissionID) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TransmissionColumns.STATUS, TransmissionStatus.SYNCED);
        final String date = String.valueOf(System.currentTimeMillis());
        contentValues.put(TransmissionColumns.START_DATE, date);
        contentValues.put(TransmissionColumns.END_DATE, date);
        database.update(Tables.TRANSMISSION, contentValues, TransmissionColumns._ID + " = ?",
                new String[] {transmissionID + ""});
    }

    public void syncSurveyedLocale(SurveyedLocale surveyedLocale) {
        final String id = surveyedLocale.getId();
        try {
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(RecordColumns.RECORD_ID, id);
            values.put(RecordColumns.SURVEY_GROUP_ID, surveyedLocale.getSurveyGroupId());
            values.put(RecordColumns.NAME, surveyedLocale.getName());
            values.put(RecordColumns.LATITUDE, surveyedLocale.getLatitude());
            values.put(RecordColumns.LONGITUDE, surveyedLocale.getLongitude());
            database.insert(Tables.RECORD, null, values);

            syncSurveyInstances(surveyedLocale.getSurveyInstances(), id);

            // Update the record last modification date, if necessary
            updateRecordModifiedDate(id, surveyedLocale.getLastModified());

            String syncTime = String.valueOf(surveyedLocale.getLastModified());
            setSyncTime(surveyedLocale.getSurveyGroupId(), syncTime);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Get the synchronization time for a particular survey group.
     *
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public String getSyncTime(long surveyGroupId) {
        Cursor cursor = database.query(Tables.SYNC_TIME,
                new String[] { SyncTimeColumns.SURVEY_GROUP_ID, SyncTimeColumns.TIME },
                SyncTimeColumns.SURVEY_GROUP_ID + "=?",
                new String[] { String.valueOf(surveyGroupId) },
                null, null, null);

        String time = null;
        if (cursor.moveToFirst()) {
            time = cursor.getString(cursor.getColumnIndexOrThrow(SyncTimeColumns.TIME));
        }
        cursor.close();
        return time;
    }

    /**
     * Save the time of synchronization time for a particular SurveyGroup
     *
     * @param surveyGroupId id of the SurveyGroup
     * @param time          String containing the timestamp
     */
    private void setSyncTime(long surveyGroupId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeColumns.TIME, time);
        database.insert(Tables.SYNC_TIME, null, values);
    }

    /**
     * Delete any SurveyInstance that contains no response.
     */
    public void deleteEmptySurveyInstances() {
        executeSql("DELETE FROM " + Tables.SURVEY_INSTANCE
                + " WHERE " + SurveyInstanceColumns._ID + " NOT IN "
                + "(SELECT DISTINCT " + ResponseColumns.SURVEY_INSTANCE_ID
                + " FROM " + Tables.RESPONSE + ")");
    }

    /**
     * Delete any Record that contains no SurveyInstance
     */
    public void deleteEmptyRecords() {
        executeSql("DELETE FROM " + Tables.RECORD
                + " WHERE " + RecordColumns.RECORD_ID + " NOT IN "
                + "(SELECT DISTINCT " + SurveyInstanceColumns.RECORD_ID
                + " FROM " + Tables.SURVEY_INSTANCE + ")");
    }

    /**
     * Query the given table, returning a Cursor over the result set.
     */
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        return database.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    // Wrap DB projections and column indexes.
    public interface RecordQuery {
        String[] PROJECTION = {
                RecordColumns._ID,
                RecordColumns.RECORD_ID,
                RecordColumns.SURVEY_GROUP_ID,
                RecordColumns.NAME,
                RecordColumns.LATITUDE,
                RecordColumns.LONGITUDE,
                RecordColumns.LAST_MODIFIED,
        };

        int _ID = 0;
        int RECORD_ID = 1;
        int SURVEY_GROUP_ID = 2;
        int NAME = 3;
        int LATITUDE = 4;
        int LONGITUDE = 5;
        int LAST_MODIFIED = 6;
    }

    public interface FormInstanceQuery {
        String[] PROJECTION = {
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.VERSION,
                SurveyColumns.NAME,
                SurveyInstanceColumns.SAVED_DATE,
                SurveyInstanceColumns.USER_ID,
                SurveyInstanceColumns.SUBMITTED_DATE,
                SurveyInstanceColumns.UUID,
                SurveyInstanceColumns.STATUS,
                SurveyInstanceColumns.SYNC_DATE,
                SurveyInstanceColumns.EXPORTED_DATE,
                SurveyInstanceColumns.RECORD_ID,
                SurveyInstanceColumns.SUBMITTER,
        };

        int _ID = 0;
        int SURVEY_ID = 1;
        int VERSION = 2;
        int NAME = 3;
        int SAVED_DATE = 4;
        int USER_ID = 5;
        int SUBMITTED_DATE = 6;
        int UUID = 7;
        int STATUS = 8;
        int SYNC_DATE = 9;
        int EXPORTED_DATE = 10;
        int RECORD_ID = 11;
        int SUBMITTER = 12;
    }

}
