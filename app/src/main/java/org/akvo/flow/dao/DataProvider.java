package org.akvo.flow.dao;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

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
                
                final String term = selectionArgs[0] + "%";
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
                    new String[]{String.valueOf(surveyGroupId), term, term},
                    null, null, sortOrder);
                break;
        }
        
        if (cursor != null) {
            // Sets the ContentResolver to watch this content URI for data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
        return cursor;    
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
