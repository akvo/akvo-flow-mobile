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

public class OfflineAreasListAdapter
        extends RecyclerView.Adapter<OfflineAreasListAdapter.ViewHolder> {

    private static final int INVALID_POSITION = -1;

    private final List<ListOfflineArea> offlineAreas;
    private final OfflineAreasActionListener listener;

    public OfflineAreasListAdapter(ArrayList<ListOfflineArea> offlineAreas,
            OfflineAreasActionListener listener) {
        this.offlineAreas = offlineAreas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.offline_area_list_item, parent, false);
        return new ViewHolder(view, listener);
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

    public void updateOfflineArea(ListOfflineArea offlineArea) {
        int i = offlineAreas == null ? INVALID_POSITION : offlineAreas.indexOf(offlineArea);
        if (i != INVALID_POSITION) {
            offlineAreas.remove(offlineArea);
            offlineAreas.add(i, offlineArea);
            notifyItemChanged(i);
        }
    }

    public void updateDisplayedName(long areaId, String newName) {
        ListOfflineArea offlineArea = getItem(areaId);
        if (offlineArea != null) {
            int i = offlineAreas.indexOf(offlineArea);
            offlineAreas.remove(offlineArea);
            offlineAreas.add(i, new ListOfflineArea(areaId, newName, offlineArea.getSize(),
                    offlineArea.isDownloading(), offlineArea.isAvailable(),
                    offlineArea.getMapInfo()));
            notifyItemChanged(i);
        }
    }

    private ListOfflineArea getItem(long areaId) {
        for(ListOfflineArea listOfflineArea: offlineAreas) {
            if (listOfflineArea.getId() == areaId) {
                return listOfflineArea;
            }
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTv;
        private final TextView stateTv;
        private final ProgressBar downloadProgress;
        private final Button selectBt;
        private final ImageButton revealMenuBt;
        private final OfflineAreasActionListener listener;

        ViewHolder(View itemView, OfflineAreasActionListener listener) {
            super(itemView);
            this.nameTv = itemView.findViewById(R.id.title_tv);
            this.stateTv = itemView.findViewById(R.id.subtitle_tv);
            this.downloadProgress = itemView.findViewById(R.id.loading_pb);
            this.selectBt = itemView.findViewById(R.id.select_bt);
            this.revealMenuBt = itemView.findViewById(R.id.display_menu_bt);
            this.listener = listener;
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
                if (offlineArea.isAvailable()) {
                    selectBt.setEnabled(true);
                    revealMenuBt.setEnabled(true);
                } else {
                    selectBt.setEnabled(false);
                    revealMenuBt.setEnabled(false);
                }
                selectBt.setOnClickListener(v -> {
                    listener.selectArea(offlineArea.getId());
                });
                revealMenuBt.setOnClickListener(v -> showMenu(revealMenuBt, offlineArea));
            }
        }

        void showMenu(View anchor, ListOfflineArea offlineArea) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.offline_area_popup, popup.getMenu());
            popup.show();
            popup.setOnMenuItemClickListener(item -> onMenuItemClicked(item, offlineArea));
        }

        private boolean onMenuItemClicked(MenuItem item, ListOfflineArea offlineArea) {
            switch (item.getItemId()) {
                case R.id.view_area:
                    listener.viewArea(offlineArea.getName(), offlineArea.getMapInfo());
                break;
                case R.id.rename_area:
                    listener.renameArea(offlineArea.getId(), offlineArea.getName());
                    break;
                case R.id.delete_area:
                    listener.deleteArea(offlineArea.getId(), offlineArea.getName());
                    break;
                    default:
                        break;
            }
            return false;
        }
    }
}
