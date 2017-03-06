/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.LanguageTable;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.Tables;

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
        Context context = getContext();
        mDatabaseHelper = new DatabaseHelper(context, new LanguageTable(),
                new FlowMigrationListener(new Prefs(context),
                        new MigrationLanguageMapper(context)));
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
