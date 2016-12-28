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

package org.akvo.flow.data.database;

/**
 * Submitter is a denormalized value of the user_id.name in locally created surveys, whereas
 * on synced surveys, it just represents the name of the submitter (not matching a local user).
 * This is just a temporary implementation before a more robust login system is integrated.
 */
public interface SurveyInstanceColumns {
    String _ID = "_id";
    String UUID = "uuid";
    String SURVEY_ID = "survey_id";
    String USER_ID = "user_id";
    String RECORD_ID = "surveyed_locale_id";
    String START_DATE = "start_date";
    String SAVED_DATE = "saved_date";
    String SUBMITTED_DATE = "submitted_date";
    String EXPORTED_DATE = "exported_date";
    String SYNC_DATE = "sync_date";
    /**
     * Denormalized value. see {@link SurveyInstanceStatus}
     **/
    String STATUS = "status";
    String DURATION = "duration";
    String SUBMITTER = "submitter";// Submitter name. Added in DB version 79
    String VERSION = "version";
}
