/*
 * Copyright (C) 2017-2019,2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.database.cascade;

import android.database.Cursor;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import org.akvo.flow.domain.Node;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class NodeMapper {

    NodeMapper() {
    }

    @NonNull
    List<Node> mapNodes(@NotNull Cursor c) {
        List<Node> result = new ArrayList<>(c.getCount());
        if (c.moveToFirst()) {
            final int codeCol = c.getColumnIndex(NodeColumns.CODE);
            do {
                Node node = mapNode(c, codeCol);
                result.add(node);
            } while (c.moveToNext());
        }
        c.close();
        return result;
    }

    @NonNull
    SparseArray<List<Node>> nodesAsMap(@NotNull Cursor c) {
        SparseArray<List<Node>> resultMap = new SparseArray<>();
        List<Node> nodes = mapNodes(c);
        for (Node node : nodes) {
            int parentId = (int) node.getParent();
            List<Node> parentNodes = resultMap.get(parentId);
            if (parentNodes == null) {
                parentNodes = new ArrayList<>();
            }
            parentNodes.add(node);
            resultMap.put(parentId, parentNodes);
        }
        return resultMap;
    }

    @NonNull
    private Node mapNode(Cursor c, int codeColumnIdx) {
        long id = c.getLong(c.getColumnIndex(NodeColumns.ID));
        String name = c.getString(c.getColumnIndex(NodeColumns.NAME));
        String code = codeColumnIdx > -1 ? c.getString(codeColumnIdx) : null;
        long parent = c.getLong(c.getColumnIndex(NodeColumns.PARENT));
        return new Node(id, name, code, parent);
    }
}
