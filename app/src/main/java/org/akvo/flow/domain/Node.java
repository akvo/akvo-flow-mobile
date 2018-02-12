/*
 *  Copyright (C) 2014-2015,2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

/**
 * Node represents a cascading question tree value.
 */
public class Node {
    private final long mId;
    private final String mName;
    private final String mCode;
    private final long parent;

    public Node(long id, String name, String code, long parent) {
        this.mId = id;
        this.mName = name;
        this.mCode = code;
        this.parent = parent;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getCode() {
        return mCode;
    }

    @Override
    public String toString() {
        return mName;
    }

    public long getParent() {
        return parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        return mId == node.mId && parent == node.parent && mName.equals(node.mName) && mCode
                .equals(node.mCode);
    }

    @Override
    public int hashCode() {
        int result = (int) (mId ^ (mId >>> 32));
        result = 31 * result + mName.hashCode();
        result = 31 * result + mCode.hashCode();
        result = 31 * result + (int) (parent ^ (parent >>> 32));
        return result;
    }
}
