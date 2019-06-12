/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.presentation.datapoints.map.offline.list;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineArea;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OfflineAreasListAdapter extends RecyclerView.Adapter<OfflineAreasListAdapter.ViewHolder> {

    private final List<ListOfflineArea> offlineAreas;

    public OfflineAreasListAdapter(ArrayList<ListOfflineArea> offlineAreas) {
        this.offlineAreas = offlineAreas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.offline_area_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setTextView(offlineAreas.get(position));
    }

    public void setOfflineAreas(@NonNull List<ListOfflineArea> results) {
        offlineAreas.clear();
        offlineAreas.addAll(results);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return offlineAreas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTv;
        private final TextView stateTv;
        private final ProgressBar downloadProgress;
        private final Button selectBt;
        private final ImageButton revealMenuBt;

        ViewHolder(View itemView) {
            super(itemView);
            this.nameTv = itemView.findViewById(R.id.title_tv);
            this.stateTv = itemView.findViewById(R.id.subtitle_tv);
            this.downloadProgress = itemView.findViewById(R.id.loading_pb);
            this.selectBt = itemView.findViewById(R.id.select_bt);
            this.revealMenuBt = itemView.findViewById(R.id.display_menu_bt);
        }

        void setTextView(ListOfflineArea offlineArea) {
            if (offlineArea != null) {
                nameTv.setText(offlineArea.getName());
                if (offlineArea.isDownloading()) {
                    stateTv.setText(nameTv.getContext().getString(R.string.offline_item_status));
                    downloadProgress.setVisibility(View.VISIBLE);
                } else {
                    stateTv.setText(offlineArea.getSize());
                    downloadProgress.setVisibility(View.GONE);
                }
                selectBt.setOnClickListener(v -> {
                    //TODO:
                });
                revealMenuBt.setOnClickListener(v -> showMenu(revealMenuBt));
            }
        }

        void showMenu(View anchor) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.offline_area_popup, popup.getMenu());
            popup.show();
            popup.setOnMenuItemClickListener(this::onMenuItemClicked);
        }

        private boolean onMenuItemClicked(MenuItem item) {
            //TODO:
            return false;
        }
    }
}
