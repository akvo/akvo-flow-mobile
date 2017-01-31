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

package org.akvo.flow.data.database;

/**
 * Submitter is a denormalized value of the user_id.name in locally created surveys, whereas
 * on synced surveys, it just represents the name of the submitter (not matching a local user).
 * This is just a temporary implementation before a more robust login system is integrated.
 */
public class SurveyInstanceColumns {
    public static final String _ID = "_id";
    public static final String UUID = "uuid";
    public static final String SURVEY_ID = "survey_id";
    public static final String USER_ID = "user_id";
    public static final String RECORD_ID = "surveyed_locale_id";
    public static final String START_DATE = "start_date";
    public static final String SAVED_DATE = "saved_date";
    public static final String SUBMITTED_DATE = "submitted_date";
    public static final String EXPORTED_DATE = "exported_date";
    public static final String SYNC_DATE = "sync_date";
    /**
     * Denormalized value. see {@link SurveyInstanceStatus}
     **/
    public static final String STATUS = "status";
    public static final String DURATION = "duration";
    public static final String SUBMITTER = "submitter";// Submitter name. Added in DB version 79
    public static final String VERSION = "version";
}
