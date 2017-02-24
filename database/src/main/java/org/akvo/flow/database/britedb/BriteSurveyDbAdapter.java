/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database.britedb;

import android.content.ContentValues;
import android.database.Cursor;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.Tables;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

import static org.akvo.flow.database.Constants.ORDER_BY_DATE;
import static org.akvo.flow.database.Constants.ORDER_BY_DISTANCE;
import static org.akvo.flow.database.Constants.ORDER_BY_NAME;
import static org.akvo.flow.database.Constants.ORDER_BY_STATUS;

public class BriteSurveyDbAdapter {

    private final BriteDatabase briteDatabase;

    public BriteSurveyDbAdapter(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    /**
     * Filters surveyd locales based on the parameters passed in.
     */
    public Observable<Cursor> getFilteredSurveyedLocales(long surveyGroupId, Double latitude,
            Double longitude,
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
            case ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";// By date
                break;
            case ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null) {
                    // this is to correct the distance for the shortening at higher latitudes
                    Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)), 2);

                    // this uses a simple planar approximation of distance. this should be good
                    // enough for our purpose.
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
            case ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " COLLATE NOCASE ASC";// By name
                break;
        }

        String[] whereValues = new String[] { String.valueOf(surveyGroupId) };
        List<String> tables = new ArrayList<>(2);
        tables.add(Tables.RECORD);
        tables.add(Tables.SURVEY_INSTANCE);
        return briteDatabase
                .createQuery(tables, queryString + whereClause + groupBy + orderByStr, whereValues)
                .concatMap(
                        new Func1<SqlBrite.Query, Observable<Cursor>>() {
                            @Override
                            public Observable<Cursor> call(SqlBrite.Query query) {
                                return Observable.just(query.run());
                            }
                        });
    }

    public Observable<Cursor> getSurveyedLocales(long surveyGroupId) {
        String sqlQuery = "SELECT * FROM " + Tables.RECORD + " WHERE "+RecordColumns.SURVEY_GROUP_ID+" = ?";
        return briteDatabase.createQuery(Tables.RECORD, sqlQuery,
                new String[] { String.valueOf(surveyGroupId) }).concatMap(
                new Func1<SqlBrite.Query, Observable<? extends Cursor>>() {
                    @Override public Observable<? extends Cursor> call(SqlBrite.Query query) {
                        return Observable.just(query.run());
                    }
                });
    }

    /**
     * Update the last modification date, if necessary
     */
    public void updateRecordModifiedDate(String recordId, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.LAST_MODIFIED, timestamp);
        briteDatabase.update(Tables.RECORD, values,
                RecordColumns.RECORD_ID + " = ? AND " + RecordColumns.LAST_MODIFIED + " < ?",
                new String[] { recordId, String.valueOf(timestamp) });
    }

    public void updateSurveyedLocale(String surveyedLocaleId, ContentValues surveyedLocaleValues) {
        briteDatabase.update(Tables.RECORD, surveyedLocaleValues,
                RecordColumns.RECORD_ID + " = ?",
                new String[] { surveyedLocaleId });
    }

    private void insertRecord(ContentValues values) {
        briteDatabase.insert(Tables.RECORD, values);
    }

    public void updateRecord(String id, ContentValues values, long lastModified) {
        BriteDatabase.Transaction transaction = briteDatabase.newTransaction();
        try {
            insertRecord(values); //TODO: should it be insert or update??
            // Update the record last modification date, if necessary
            updateRecordModifiedDate(id, lastModified);
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    /**
     * Get the synchronization time for a particular survey group.
     *
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public Observable<Cursor> getSyncTime(long surveyGroupId) {
        String sql =
                "SELECT * FROM " + Tables.SYNC_TIME + " WHERE " + SyncTimeColumns.SURVEY_GROUP_ID
                        + " = ?";
        Cursor cursor = briteDatabase.query(sql, new String[] { String.valueOf(surveyGroupId) });
        return Observable.just(cursor);
    }

    public void insertSyncedTime( ContentValues values) {
        briteDatabase.insert(Tables.SYNC_TIME, values);
    }
}
