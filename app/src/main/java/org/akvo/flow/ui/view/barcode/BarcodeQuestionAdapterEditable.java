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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarcodeQuestionAdapterEditable extends
        RecyclerView.Adapter<ViewHolder> {

    private static final int VIEW_TYPE_LAST_ITEM = 0;
    private static final int VIEW_TYPE_LAST_ITEM_LOCKED = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    @NonNull
    private final List<String> barCodes = new ArrayList<>();

    @Nullable
    private final MultiQuestionListener multiQuestionListener;

    @Nullable
    private final ScanButton.ScanButtonListener scanButtonListener;

    private final boolean isLocked;

    BarcodeQuestionAdapterEditable(List<String> barCodes,
            BarcodeQuestionViewMultiple barcodeQuestionViewMultiple, boolean isLocked) {
        this.multiQuestionListener = barcodeQuestionViewMultiple;
        this.scanButtonListener = barcodeQuestionViewMultiple;
        this.isLocked = isLocked;
        this.barCodes.addAll(barCodes);
        this.barCodes.add("");
    }

    void addBarCode(String barcode) {
        int index = Math.max(barCodes.size() - 1, 0);
        this.barCodes.add(index, barcode);
        this.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view;
        if (viewType == VIEW_TYPE_LAST_ITEM) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.barcode_item_last, parent, false);
            return new LastViewHolder(view, scanButtonListener, multiQuestionListener);
        } else if (viewType == VIEW_TYPE_LAST_ITEM_LOCKED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.barcode_item_last_locked, parent, false);
            return new LastViewHolderLocked(view, scanButtonListener);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.barcode_item, parent, false);
            return new OtherViewHolder(view, multiQuestionListener);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof LastViewHolder) {
            ((LastViewHolder) holder).setUpViews(barCodes.get(position));
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).setUpViews(barCodes.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        return barCodes.size();
    }

    @Override
    public int getItemViewType(int position) {
        boolean isLastItem = position == getItemCount() - 1;
        if (isLastItem) {
            return isLocked ? VIEW_TYPE_LAST_ITEM_LOCKED : VIEW_TYPE_LAST_ITEM;
        }
        return VIEW_TYPE_OTHER;
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
        if (barCodes.size() == 0) {
            barCodes.add("");
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
        barCodes.add("");
        notifyDataSetChanged();
    }

    static class LastViewHolder extends ViewHolder {

        private final EditText barcodeEdit;
        private final ImageButton addButton;
        private final ScanButton scanButton;
        private final MultiQuestionListener listener;

        LastViewHolder(View itemView, ScanButton.ScanButtonListener scanButtonListener,
                MultiQuestionListener multiQuestionListener) {
            super(itemView);
            this.barcodeEdit = (EditText) itemView.findViewById(R.id.input);
            this.addButton = (ImageButton) itemView.findViewById(R.id.add_btn);
            this.scanButton = (ScanButton) itemView.findViewById(R.id.scan_btn);
            this.listener = multiQuestionListener;
            scanButton.setListener(scanButtonListener);
        }

        void setUpViews(String text) {
            barcodeEdit.setText(text);
            barcodeEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                        int after) {
                    //EMPTY
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before,
                        int count) {
                    //EMPTY
                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateAddButton();
                }
            });
            updateAddButton();
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onQuestionAddTap(barcodeEdit.getText().toString());
                    }
                }
            });
        }

        private void updateAddButton() {
            addButton.setEnabled(!isBarcodeEmpty());
        }

        private boolean isBarcodeEmpty() {
            return TextUtils.isEmpty(barcodeEdit.getText().toString());
        }
    }

    static class LastViewHolderLocked extends ViewHolder {

        private final ScanButton scanButton;

        LastViewHolderLocked(View itemView, ScanButton.ScanButtonListener scanButtonListener) {
            super(itemView);
            this.scanButton = (ScanButton) itemView.findViewById(R.id.scan_btn);
            scanButton.setListener(scanButtonListener);
        }
    }
}
