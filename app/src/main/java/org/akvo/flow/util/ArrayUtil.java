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

import java.util.List;

/**
 * Utilities for manipulating arrays
 * 
 * @author Christopher Fagiani
 */
public class ArrayUtil {

    /**
     * copies the values from the source array into the destination array
     * starting at index startIndex.
     * 
     * @param destination
     * @param source
     * @param startIndex
     * @throws IllegalArgumentException - if the destination array isn't large
     *             enough to accommodate all elements in the source
     */
    public static void combineArrays(Object[] destination, Object[] source,
            int startIndex) {
        if (destination.length < startIndex + source.length) {
            throw new IllegalArgumentException(
                    "Destination array is of insufficient size");
        }
        for (int i = 0; i < source.length; i++) {
            destination[i + startIndex] = source[i];
        }
    }

    public static boolean[] toPrimitiveBooleanArray(final List<Boolean> booleanList) {
        final boolean[] primitives = new boolean[booleanList.size()];
        int index = 0;
        for (Boolean object : booleanList) {
            primitives[index++] = object;
        }
        return primitives;
    }

    public static int[] toPrimitiveIntArray(final List<Integer> integerList) {
        final int[] primitives = new int[integerList.size()];
        int index = 0;
        for (Integer object : integerList) {
            primitives[index++] = object;
        }
        return primitives;
    }

}
