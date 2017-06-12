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
import android.widget.TextView;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarcodeQuestionAdapter
        extends RecyclerView.Adapter<BarcodeQuestionAdapter.ViewHolder> {

    private static final int VIEW_TYPE_LAST_ITEM = 0;
    private static final int VIEW_TYPE_OTHER = 2;

    @NonNull
    private final List<String> barCodes;

    @Nullable
    private final MultiQuestionListener multiQuestionListener;

    @Nullable
    private final ScanButton.ScanButtonListener scanButtonListener;

    BarcodeQuestionAdapter(List<String> barCodes,
            BarcodeQuestionViewMultiple barcodeQuestionViewMultiple) {
        this.multiQuestionListener = barcodeQuestionViewMultiple;
        this.scanButtonListener = barcodeQuestionViewMultiple;
        this.barCodes = new ArrayList<>();
        this.barCodes.addAll(barCodes);
        this.barCodes.add(""); //the last item
    }

    void addBarCodes(@Nullable String[] barCodes) {
        if (barCodes == null || barCodes.length == 0) {
            return;
        }
        this.barCodes.addAll(0, Arrays.asList(barCodes));
        this.notifyDataSetChanged();
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
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_LAST_ITEM;
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

    static abstract class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
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

    static class OtherViewHolder extends ViewHolder {

        final EditText barcodeEdit;
        private final TextView positionTextView;
        private final ImageButton deleteButton;
        private final MultiQuestionListener listener;

        OtherViewHolder(View itemView, MultiQuestionListener listener) {
            super(itemView);
            this.barcodeEdit = (EditText) itemView.findViewById(R.id.input);
            this.positionTextView = (TextView) itemView.findViewById(R.id.order);
            this.deleteButton = (ImageButton) itemView.findViewById(R.id.delete);
            this.listener = listener;
        }

        void setUpViews(String text, final int position) {
            barcodeEdit.setText(text);
            positionTextView.setText(position + "");
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onQuestionRemoveTap(position);
                    }
                }
            });
        }
    }

    interface MultiQuestionListener {

        void onQuestionAddTap(String text);

        void onQuestionRemoveTap(int position);
    }
}
