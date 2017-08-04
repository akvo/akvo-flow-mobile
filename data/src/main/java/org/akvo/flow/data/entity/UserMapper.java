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

package org.akvo.flow.data.entity;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.entity.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class UserMapper {

    @Inject
    public UserMapper() {
    }

    private User getUser(@NonNull Cursor cursor) {
        long id = cursor.getLong(
                cursor.getColumnIndexOrThrow(UserColumns._ID));
        String name = cursor.getString(
                cursor.getColumnIndexOrThrow(UserColumns.NAME));
        return new User(id, name);
    }

    @NonNull
    public List<User> getUsers(@Nullable Cursor cursor) {
        List<User> users = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                   users.add(getUser(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return users;
    }
}
