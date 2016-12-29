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

public class TransmissionColumns {
    public static final String _ID = "_id";
    public static final String SURVEY_INSTANCE_ID = "survey_instance_id";
    public static final String SURVEY_ID = "survey_id";
    public static final String FILENAME = "filename";
    public static final String STATUS = "status";// separate table/constants?
    public static final String START_DATE = "start_date";// do we really need this column?
    public static final String END_DATE = "end_date";
}
