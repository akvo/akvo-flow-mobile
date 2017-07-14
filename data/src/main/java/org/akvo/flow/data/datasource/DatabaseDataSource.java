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

package org.akvo.flow.data.datasource;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.sqlbrite.BriteDatabase;

import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiQuestionAnswer;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.database.Constants;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.britedb.BriteSurveyDbAdapter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

public class DatabaseDataSource {

    private final BriteSurveyDbAdapter briteSurveyDbAdapter;

    @Inject
    public DatabaseDataSource(BriteDatabase db) {
        this.briteSurveyDbAdapter = new BriteSurveyDbAdapter(db);
    }
}