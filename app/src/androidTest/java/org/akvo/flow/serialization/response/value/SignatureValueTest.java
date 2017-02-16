/*
 * Copyright (C) 2015-2017 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.response.value.Signature;

public class SignatureValueTest extends TestCase {

    public void testInvalid() {
        // Ensure the signature deserialization never returns null
        Signature signature = SignatureValue.deserialize("");
        assertNotNull(signature);
        assertNull(signature.getName());
        assertNull(signature.getImage());
    }

    public void testSerialization() {
        Signature signature = new Signature();
        signature.setName("Bob");
        signature.setImage("aaa");

        String value = SignatureValue.serialize(signature);

        Signature deserialized = SignatureValue.deserialize(value);

        assertEquals(signature.getName(), deserialized.getName());
        assertEquals(signature.getImage(), deserialized.getImage());
    }
}
