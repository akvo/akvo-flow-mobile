/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.offlinemaps;

import com.mapbox.mapboxsdk.maps.Style;

public class Constants {

    public static final String MAPBOX_MAP_STYLE = Style.MAPBOX_STREETS;
    public static final int MAP_BOX_ZOOM_MAX = 2;
    public static final String LATITUDE_PROPERTY = "latitude";
    public static final String LONGITUDE_PROPERTY = "longitude";
    public static final String ID_PROPERTY = "id";
    public static final String NAME_PROPERTY = "name";

    public static final String CALLING_SCREEN_EXTRA = "calling-screen-extra";
    public static final int CALLING_SCREEN_EXTRA_DIALOG = 0;
    public static final int CALLING_SCREEN_EXTRA_LIST = 1;
}
