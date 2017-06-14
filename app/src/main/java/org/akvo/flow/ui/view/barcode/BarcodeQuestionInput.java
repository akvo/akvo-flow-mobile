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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.akvo.flow.R;

public abstract class BarcodeQuestionInput extends LinearLayout {

    EditText barcodeEdit;
    ImageButton addButton;
    ScanButton scanButton;
    View manualInputSeparator;

    @Nullable
    BarcodeEditListener barcodeEditListener;

    @Nullable
    private ScanButton.ScanButtonListener scanButtonListener;

    @Nullable
    AddButtonListener addButtonListener;

    public BarcodeQuestionInput(Context context) {
        this(context, null);
    }

    public BarcodeQuestionInput(Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.barcode_question_input, this);
        this.barcodeEdit = (EditText) findViewById(R.id.input);
        this.addButton = (ImageButton) findViewById(R.id.add_btn);
        this.scanButton = (ScanButton) findViewById(R.id.scan_btn);
        this.manualInputSeparator = findViewById(R.id.manual_input_separator);
    }

    public void setBarcodeEditListener(BarcodeEditListener barcodeEditListener) {
        this.barcodeEditListener = barcodeEditListener;
    }

    abstract void initViews();

    public void setScanButtonListener(@Nullable ScanButton.ScanButtonListener scanButtonListener) {
        this.scanButtonListener = scanButtonListener;
        if (scanButton != null) {
            scanButton.setListener(scanButtonListener);
        }
    }

    public void setAddButtonListener(@Nullable AddButtonListener addButtonListener) {
        this.addButtonListener = addButtonListener;
    }

    void setUpViews(String text, boolean isReadOnly, boolean isMultiEntry, boolean isLocked) {
        if (isReadOnly) {
            setUpReadOnlyViews(text);
            return;
        } else if (isLocked) {
            setUpLockedViews();
            return;
        } else {
            updateViewsNormal(text, isMultiEntry);
        }
    }

    private void updateViewsNormal(String text, boolean isMultiEntry) {
        barcodeEdit.setVisibility(VISIBLE);
        manualInputSeparator.setVisibility(VISIBLE);
//        barcodeEdit.addTextChangedListener(new BarcodeEditWatcher());
        if (isMultiEntry) {
            addButton.setVisibility(VISIBLE);
            addButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (addButtonListener != null) {
                        addButtonListener.onQuestionAddTap(getBarcode());
                    }
                }
            });
        } else {
            addButton.setVisibility(GONE);
            barcodeEdit.setText(text);
        }
    }

    private void setUpLockedViews() {
        barcodeEdit.setVisibility(GONE);
        manualInputSeparator.setVisibility(GONE);
        addButton.setVisibility(GONE);
        scanButton.setVisibility(VISIBLE);
    }

    private void setUpReadOnlyViews(String text) {
        barcodeEdit.setText(text);
        barcodeEdit.setVisibility(VISIBLE);
        barcodeEdit.setEnabled(false);
        barcodeEdit.setFocusable(false);
        addButton.setVisibility(GONE);
        scanButton.setVisibility(GONE);
        manualInputSeparator.setVisibility(GONE);
    }

    public void updateAddButton() {
        if (addButton != null) {
            addButton.setEnabled(!isBarcodeEmpty());
        }
    }

    private boolean isBarcodeEmpty() {
        return TextUtils.isEmpty(getBarcode());
    }

    @NonNull
    String getBarcode() {
        return barcodeEdit.getText().toString();
    }

    abstract void setBarcodeText(String value);

    public interface AddButtonListener {

        void onQuestionAddTap(String text);
    }

    public interface BarcodeEditListener {

        void onTextEdited(String text);
    }

    public static abstract class BarcodeEditWatcher implements TextWatcher {

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
    }
}
