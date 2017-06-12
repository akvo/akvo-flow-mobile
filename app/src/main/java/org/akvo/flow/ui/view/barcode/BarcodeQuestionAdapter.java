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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.akvo.flow.R;
import org.akvo.flow.ui.view.QuestionView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarcodeQuestionAdapter
        extends RecyclerView.Adapter<BarcodeQuestionAdapter.ViewHolder> {

    private static final int VIEW_TYPE_LAST_ITEM = 0;
    private static final int VIEW_TYPE_OTHER = 0;
    private final WeakReference<QuestionView> questionViewWeakRef;

    @NonNull
    private final List<String> barCodes;

    public BarcodeQuestionAdapter(List<String> barCodes, QuestionView questionView) {
        this.barCodes = new ArrayList<>();
        this.barCodes.addAll(barCodes);
        this.barCodes.add(""); //the last item
        this.questionViewWeakRef = new WeakReference<>(questionView);
    }

    public void addBarCodes(@Nullable String [] barCodes) {
        if (barCodes == null || barCodes.length == 0) {
            return;
        }
        this.barCodes.addAll(0, Arrays.asList(barCodes));
        notifyItemRangeInserted(0, barCodes.length);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view;
        if (viewType == VIEW_TYPE_LAST_ITEM) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.barcode_item_last, parent, false);
            return new LastViewHolder(view, questionViewWeakRef);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.barcode_item, parent, false);
            return new OtherViewHolder(view, questionViewWeakRef);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setUpViews(barCodes.get(position));
    }

    @Override
    public int getItemCount() {
        return barCodes.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_LAST_ITEM;
        }
        return VIEW_TYPE_OTHER;
    }

    public String getBarCodes() {
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

    static abstract class ViewHolder extends RecyclerView.ViewHolder {

        final EditText barcodeEdit;

        ViewHolder(View itemView, WeakReference<QuestionView> questionViewWeakRef) {
            super(itemView);
            this.barcodeEdit = (EditText) itemView.findViewById(R.id.input);
            barcodeEdit.addTextChangedListener(new ResponseInputWatcher(questionViewWeakRef));
        }
        abstract void setUpViews(String text);
    }

    static class LastViewHolder extends ViewHolder {

        private final ImageButton addButton;
        private final Button scanButton;

        LastViewHolder(View itemView, WeakReference<QuestionView> questionViewWeakRef) {
            super(itemView, questionViewWeakRef);
            this.addButton = (ImageButton) itemView.findViewById(R.id.add_btn);
            this.scanButton = (Button) itemView.findViewById(R.id.scan_btn);
        }

        @Override
        void setUpViews(String text) {
            barcodeEdit.setText(text);
            addButton.setEnabled(!TextUtils.isEmpty(text));
        }
    }

    static class OtherViewHolder extends ViewHolder {

        private final ImageButton deleteButton;

        OtherViewHolder(View itemView, WeakReference<QuestionView> questionViewWeakRef) {
            super(itemView, questionViewWeakRef);
            this.deleteButton = (ImageButton) itemView.findViewById(R.id.delete);
        }

        @Override
        void setUpViews(String text) {
            barcodeEdit.setText(text);
            deleteButton.setEnabled(!TextUtils.isEmpty(text));
        }
    }

}
