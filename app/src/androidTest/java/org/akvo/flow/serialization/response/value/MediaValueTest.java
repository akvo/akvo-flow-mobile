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
package org.akvo.flow.serialization.response.value;

import junit.framework.TestCase;

import org.akvo.flow.domain.response.value.Location;
import org.akvo.flow.domain.response.value.Media;

public class MediaValueTest extends TestCase {

    public void testInvalid() {
        Media media = MediaValue.deserialize("");
        assertNull(media);
    }

    public void testSerialization() {
        Media media = new Media();
        Location location = new Location(40.0, 2.0, 1.0, 5f);
        media.setFilename("file1");
        media.setLocation(location);

        String value = MediaValue.serialize(media, false);

        Media deserialized = MediaValue.deserialize(value);

        assertEquals(media.getFilename(), deserialized.getFilename());
        assertEquals(media.getLocation().getAccuracy(), deserialized.getLocation().getAccuracy());
        assertEquals(media.getLocation().getAltitude(), deserialized.getLocation().getAltitude());
        assertEquals(media.getLocation().getLatitude(), deserialized.getLocation().getLatitude());
        assertEquals(media.getLocation().getLongitude(), deserialized.getLocation().getLongitude());
    }

    public void testDeserializeOldFormat() {
        String fileName = "file1";
        Media deserialized = MediaValue.deserialize(fileName);
        assertEquals(fileName, deserialized.getFilename());
    }
}
