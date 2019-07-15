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

package org.akvo.flow.offlinemaps.presentation.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineArea;
import org.akvo.flow.offlinemaps.presentation.OfflineMapSelectedListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OfflineAreasAdapter extends RecyclerView.Adapter<OfflineAreasAdapter.ViewHolder> {

    private final List<DomainOfflineArea> offlineAreas;
    private final OfflineMapSelectedListener areaSelectionListener;
    private long selectedAreaId;

    public OfflineAreasAdapter(ArrayList<DomainOfflineArea> offlineAreas,
            OfflineMapSelectedListener listener) {
        this.offlineAreas = offlineAreas;
        this.areaSelectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_item_text_view, parent, false);
        return new ViewHolder(textView, areaSelectionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setTextView(offlineAreas.get(position), selectedAreaId);
    }

    public void setOfflineAreas(@NonNull List<DomainOfflineArea> results,
            long selectedAreaId) {
        this.selectedAreaId = selectedAreaId;
        offlineAreas.clear();
        offlineAreas.addAll(results);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return offlineAreas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;
        private final OfflineMapSelectedListener listener;

        ViewHolder(TextView itemView, OfflineMapSelectedListener listener) {
            super(itemView);
            this.textView = itemView;
            this.listener = listener;
        }

        void setTextView(DomainOfflineArea offlineArea, long selectedAreaId) {
            if (offlineArea != null) {
                textView.setText(offlineArea.getName());
                if (selectedAreaId == offlineArea.getId()) {
                    textView.setSelected(true);
                } else {
                    textView.setSelected(false);
                }
                textView.setOnClickListener(v -> onOfflineAreaPressed(offlineArea));
            }
        }

        private void onOfflineAreaPressed(DomainOfflineArea offlineArea) {
            if (listener != null) {
                listener.onOfflineAreaPressed(offlineArea);
            }
        }
    }
}
