/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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

public class SurveyInstanceStatus {

    /**
     * User started filling a form but did not press submit
     */
    public static final int SAVED = 0;

    /**
     * User pressed submit
     */
    public static final int SUBMIT_REQUESTED = 1;

    /**
     * Zip file has been generated
     */
    public static final int SUBMITTED = 2;

    /**
     * Datapoint has been sent to the dashboard
     */
    public static final int UPLOADED = 3;

    /**
     * Datapoint has been downloaded from the dashboard
     */
    public static final int DOWNLOADED = 4;
}
