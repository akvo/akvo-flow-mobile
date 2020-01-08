/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.navigation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.domain.entity.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class UserMapper {

    @Inject
    public UserMapper() {
    }

    @Nullable
    public ViewUser transform(@Nullable User user) {
        if (user == null) {
            return null;
        }
        return new ViewUser(user.getId(), user.getName());
    }

    @Nullable
    public User transform(@Nullable ViewUser user) {
        if (user == null) {
            return null;
        }
        return new User(user.getId(), user.getName());
    }

    @NonNull
    public List<ViewUser> transform(List<User> users) {
        List<ViewUser> viewUsers = new ArrayList<>();
        for (User u : users) {
            ViewUser viewUser = transform(u);
            if (viewUser != null) {
                viewUsers.add(viewUser);
            }
        }
        return viewUsers;
    }
}
