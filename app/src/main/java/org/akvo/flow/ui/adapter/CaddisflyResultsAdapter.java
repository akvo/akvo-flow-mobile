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

package org.akvo.flow.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.ui.model.caddisfly.CaddisflyTestResult;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CaddisflyResultsAdapter extends RecyclerView.Adapter<CaddisflyResultsAdapter.ViewHolder> {

    private final ArrayList<CaddisflyTestResult> caddisflyTestResults;

    public CaddisflyResultsAdapter(ArrayList<CaddisflyTestResult> caddisflyTestResults) {
        this.caddisflyTestResults = caddisflyTestResults;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.caddisfly_question_view_result_item, parent, false);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setTextView(caddisflyTestResults.get(position));
    }

    public void setCaddisflyTestResults(@NonNull List<CaddisflyTestResult> results) {
        if (results.size() > 0) {
            caddisflyTestResults.clear();
            caddisflyTestResults.addAll(results);
            int size = results.size();
            notifyItemRangeChanged(0, size);
        } else {
            int size = caddisflyTestResults.size();
            if (size > 0) {
                caddisflyTestResults.clear();
                notifyItemRangeRemoved(0, size);
            }
        }
    }

    @Override
    public int getItemCount() {
        return caddisflyTestResults.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        ViewHolder(TextView itemView) {
            super(itemView);
            this.textView = itemView;
        }

        void setTextView(CaddisflyTestResult result) {
            if (result != null) {
                textView.setText(result.buildResultToDisplay());
            }
        }

    }
}
