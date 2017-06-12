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

package org.akvo.flow.ui.view.barcode;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadOnlyBarcodeQuestionAdapter
        extends RecyclerView.Adapter<ReadOnlyBarcodeQuestionAdapter.ReadOnlyViewHolder> {

    @NonNull
    final List<String> barCodes = new ArrayList<>();

    void addBarCodes(@Nullable String[] barCodes) {
        if (barCodes == null || barCodes.length == 0) {
            return;
        }
        this.barCodes.addAll(0, Arrays.asList(barCodes));
        this.notifyDataSetChanged();
    }

    @Override
    public ReadOnlyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.barcode_item_read_only, parent, false);
        return new ReadOnlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReadOnlyViewHolder holder, int position) {
        holder.setUpViews(barCodes.get(position), position);
    }

    @Override
    public int getItemCount() {
        return barCodes.size();
    }

    static class ReadOnlyViewHolder extends ViewHolder {

        final EditText barcodeEdit;
        private final TextView positionTextView;

        ReadOnlyViewHolder(View itemView) {
            super(itemView);
            this.barcodeEdit = (EditText) itemView.findViewById(R.id.input);
            this.positionTextView = (TextView) itemView.findViewById(R.id.order);
        }

        void setUpViews(String text, final int position) {
            barcodeEdit.setText(text);
            positionTextView.setText(position + 1 + "");
        }
    }
}
