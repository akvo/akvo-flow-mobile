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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarcodeQuestionAdapter extends RecyclerView.Adapter<BarcodeViewHolder> {

    @NonNull
    private final List<String> barCodes = new ArrayList<>();

    @Nullable
    private final RemoveButtonListener multiQuestionListener;

    BarcodeQuestionAdapter(List<String> barCodes, RemoveButtonListener removeButtonListener) {
        this.multiQuestionListener = removeButtonListener;
        this.barCodes.addAll(barCodes);
    }

    void addBarCode(String barcode) {
        this.barCodes.add(barcode);
        this.notifyDataSetChanged();
    }

    @Override
    public BarcodeViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.barcode_item, parent, false);
        return new BarcodeViewHolder(view, multiQuestionListener);
    }

    @Override
    public void onBindViewHolder(BarcodeViewHolder holder, int position) {
        holder.setUpViews(barCodes.get(position), position);
    }

    @Override
    public int getItemCount() {
        return barCodes.size();
    }

    String getBarCodes() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String barcode : barCodes) {
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
        if (position < barCodes.size()) {
            barCodes.remove(position);
        }
        notifyDataSetChanged();
    }

    void addBarCodes(@Nullable String[] barCodes) {
        if (barCodes == null || barCodes.length == 0) {
            return;
        }
        this.barCodes.addAll(0, Arrays.asList(barCodes));
        this.notifyDataSetChanged();
    }

    void clearAll() {
        barCodes.clear();
        notifyDataSetChanged();
    }
}
