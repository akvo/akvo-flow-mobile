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

package org.akvo.flow.ui.view.barcode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class BarcodeQuestionAdapter extends RecyclerView.Adapter<BarcodeViewHolder> {

    @NonNull
    private final List<String> barcodes = new ArrayList<>();

    @Nullable
    private final RemoveButtonListener removeButtonListener;

    BarcodeQuestionAdapter(List<String> barcodes, RemoveButtonListener removeButtonListener) {
        this.removeButtonListener = removeButtonListener;
        this.barcodes.addAll(barcodes);
    }

    @Override
    public BarcodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.barcode_item, parent, false);
        return new BarcodeViewHolder(view, removeButtonListener);
    }

    @Override
    public void onBindViewHolder(BarcodeViewHolder holder, int position) {
        holder.setUpViews(barcodes.get(position), position);
    }

    @Override
    public int getItemCount() {
        return barcodes.size();
    }

    void addBarcode(String barcode) {
        this.barcodes.add(barcode);
        this.notifyDataSetChanged();
    }

    void addBarcodes(@Nullable String[] barcodes) {
        if (barcodes == null || barcodes.length == 0) {
            return;
        }
        this.barcodes.addAll(Arrays.asList(barcodes));
        this.notifyDataSetChanged();
    }

    String getBarcodes() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String barcode : barcodes) {
            if (!TextUtils.isEmpty(barcode)) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("|");
                }
                stringBuilder.append(barcode);
            }
        }
        return stringBuilder.toString();
    }

    void removeBarcode(int position) {
        if (position < barcodes.size()) {
            barcodes.remove(position);
        }
        notifyDataSetChanged();
    }

    void removeBarcodes() {
        barcodes.clear();
        notifyDataSetChanged();
    }
}
