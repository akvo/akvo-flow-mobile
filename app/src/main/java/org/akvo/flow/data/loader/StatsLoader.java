/*
 *  Copyright (C) 2014-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.data.database.RecordColumns;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyDbAdapter.RecordQuery;
import org.akvo.flow.data.database.Tables;
import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.data.loader.models.Stats;

import java.util.Calendar;

public class StatsLoader extends AsyncLoader<Stats> {

    private final long mSurveyGroupId;

    private static final long WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;// Week milliseconds

    public StatsLoader(Context context, long surveyGroupId) {
        super(context);
        this.mSurveyGroupId = surveyGroupId;
    }

    @Override
    public Stats loadInBackground() {
        SurveyDbAdapter database = new SurveyDbAdapter(getContext());
        database.open();
        Stats stats = new Stats();

        Cursor c = queryRecords(database, 0);
        if (c != null) {
            stats.mTotal = c.getCount();
            c.close();
        }

        c = queryRecords(database, System.currentTimeMillis() - WEEK_IN_MS);
        if (c != null) {
            stats.mThisWeek = c.getCount();
            c.close();
        }

        // Setup a calendar with last midnight's date
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        c = queryRecords(database, calendar.getTimeInMillis());
        if (c != null) {
            stats.mToday = c.getCount();
            c.close();
        }
        database.close();
        return stats;
    }

    private Cursor queryRecords(SurveyDbAdapter db, long minDate) {
        return db.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.SURVEY_GROUP_ID + " = ? AND " + RecordColumns.LAST_MODIFIED + " > ?",
                new String[] {
                        String.valueOf(mSurveyGroupId),
                        String.valueOf(minDate)
                },
                null, null, null);
    }
}
