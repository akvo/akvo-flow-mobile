/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.dao;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter.DatabaseHelper;
import org.akvo.flow.dao.SurveyDbAdapter.RecordColumns;
import org.akvo.flow.dao.SurveyDbAdapter.Tables;

public class DataProvider extends ContentProvider {
    
    public static final String AUTHORITY = "org.akvo.flow";
    
    private static final int SEARCH_SUGGEST = 1;
    
    private static final String SUGGEST_SELECTION = RecordColumns.SURVEY_GROUP_ID
            + " = ? AND (" + RecordColumns.RECORD_ID + " LIKE ? OR "
            + RecordColumns.NAME + " Like ?)";
    
    private static final UriMatcher sUriMatcher;
    
    private DatabaseHelper mDatabaseHelper;
    
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // Do the query against a read-only version of the database
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        // Decodes the content URI and maps it to a code
        switch (sUriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                // Suggestions search
                // Adjust incoming query to become SQL text match
                long surveyGroupId = FlowApp.getApp().getSurveyGroupId();

                String nameSearchTerm = createNameSearchTerm(selectionArgs);
                String idSearchTerm = createIdSearchTerm(selectionArgs);

                projection = new String[] {
                        RecordColumns._ID,
                        RecordColumns.RECORD_ID
                        + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                        RecordColumns.NAME
                        + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
                        RecordColumns.RECORD_ID
                        + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2};
                cursor = db.query(
                    Tables.RECORD,
                    projection,
                    SUGGEST_SELECTION,
                    new String[]{String.valueOf(surveyGroupId), idSearchTerm, nameSearchTerm},
                    null, null, sortOrder);
                break;
        }
        
        if (cursor != null) {
            // Sets the ContentResolver to watch this content URI for data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
        return cursor;    
    }

    @NonNull
    private String createIdSearchTerm(@Nullable String[] selectionArgs) {
        StringBuilder recordIdSearchBuilder = new StringBuilder();
        if (selectionArgs != null && selectionArgs.length > 0) {
            recordIdSearchBuilder.append(selectionArgs[0]);
        }
        recordIdSearchBuilder.append("%");
        return recordIdSearchBuilder.toString();
    }

    @NonNull
    private String createNameSearchTerm(@Nullable String[] selectionArgs) {
        StringBuilder nameSearchBuilder = new StringBuilder();
        nameSearchBuilder.append("%");
        if (selectionArgs != null && selectionArgs.length > 0) {
            nameSearchBuilder.append(selectionArgs[0]);
        }
        nameSearchBuilder.append("%");
        return nameSearchBuilder.toString();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // No types yet (only suggestion, which is managed (semi)automatically)
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // No types yet (only suggestion, which is managed (semi)automatically)
        return null;
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // No types yet (only suggestion, which is managed (semi)automatically)
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // No types yet (only suggestion, which is managed (semi)automatically)
        return 0;
    }

}
