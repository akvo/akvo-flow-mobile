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

import org.akvo.flow.domain.response.value.CascadeNode;

import java.util.ArrayList;
import java.util.List;

public class CascadeValueTest extends TestCase {

    public void testOldFormatDeserialize() {
        List<CascadeNode> deserialized = CascadeValue.deserialize("name1|name2");

        assertNotNull(deserialized);
        assertEquals("name1", deserialized.get(0).getName());
        assertEquals("name2", deserialized.get(1).getName());
    }

    public void testSerialization() {
        CascadeNode node1 = new CascadeNode();
        node1.setCode("1");
        node1.setName("name1");
        CascadeNode node2 = new CascadeNode();
        node2.setCode("2");
        node2.setName("name2");

        List<CascadeNode> nodes = new ArrayList<>(2);
        nodes.add(node1);
        nodes.add(node2);

        String serialized = CascadeValue.serialize(nodes);
        List<CascadeNode> deserialized = CascadeValue.deserialize(serialized);

        assertEquals(2, deserialized.size());
        assertEquals(node1.getCode(), deserialized.get(0).getCode());
        assertEquals(node1.getName(), deserialized.get(0).getName());
        assertEquals(node2.getCode(), deserialized.get(1).getCode());
        assertEquals(node2.getName(), deserialized.get(1).getName());
    }
}
