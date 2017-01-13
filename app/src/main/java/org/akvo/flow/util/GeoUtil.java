/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.util;

import java.text.DecimalFormat;

/**
 * simple utility class for handling Locations
 */
public class GeoUtil {

    public static String getDisplayLength(double distance) {
        // default: km
        DecimalFormat df = new DecimalFormat("###,###.##");
        String unit = "km";
        Double factor = 0.001; // convert from meters to km

        // for distances smaller than 1 km, use meters as unit
        if (distance < 1000.0) {
            factor = 1.0;
            unit = "m";
            //df = new DecimalFormat("#"); // only whole meters
        }
        double dist = distance * factor;
        return df.format(dist) + " " + unit;
    }

    public static String getDisplayArea(double area) {
        // default: square km
        DecimalFormat df = new DecimalFormat("###,###.##");
        String unit = "km²";
        Double factor = 0.000001; // convert from m² to km²

        // for distances smaller than 1 km², use m² as unit
        if (area < 1000000.0) {
            factor = 1.0;
            unit = "m²";
            //df = new DecimalFormat("#"); // only whole meters
        }
        double dist = area * factor;
        return df.format(dist) + " " + unit;
    }

}
