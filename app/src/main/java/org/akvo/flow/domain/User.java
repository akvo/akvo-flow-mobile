/*
 *  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.domain;

import java.io.Serializable;

public class User implements Serializable {
    private long mId;
    private String mName;

    public User(long id, String name) {
        mId = id;
        mName = name;
    }
    
    public long getId() {
        return mId;
    }
    
    public String getName() {
        return mName;
    }
    
    public void setName(String name) {
        mName = name;
    }

    @Override
    public boolean equals(Object user) {
        try {
            return user != null && ((User)user).getId() == mId;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return mId+":"+mName;
    }
}
