/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<ViewUser> users = new ArrayList<>();
    private final ViewUser addUserItem;

    public UserAdapter(Context context) {
        addUserItem = new ViewUser(ViewUser.ADD_USER_ID, context.getString(R.string.new_user));
        users.add(addUserItem);
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.navigation_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        holder.setUserName(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<ViewUser> userList) {
        users.clear();
        if (userList != null && !userList.isEmpty()) {
            users.addAll(userList);
        }
        users.add(addUserItem);
        notifyDataSetChanged();
    }

    public ViewUser getItem(int position) {
        return users.get(position);
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private final TextView userNameTv;

        UserViewHolder(View itemView) {
            super(itemView);
            this.userNameTv = (TextView) itemView.findViewById(R.id.item_text_view);
        }

        void setUserName(ViewUser viewUser) {
            userNameTv.setText(viewUser.getName());
        }
    }
}
