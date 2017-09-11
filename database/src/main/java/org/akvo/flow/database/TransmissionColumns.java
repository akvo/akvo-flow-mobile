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

package org.akvo.flow.database;

public class TransmissionColumns {
    public static final String _ID = "_id";
    public static final String SURVEY_INSTANCE_ID = "survey_instance_id";
    public static final String SURVEY_ID = "survey_id";
    public static final String FILENAME = "filename";
    public static final String STATUS = "status";// separate table/constants?
    public static final String START_DATE = "start_date";// do we really need this column?
    public static final String END_DATE = "end_date";
}
