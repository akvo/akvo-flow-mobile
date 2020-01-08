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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.akvo.flow.R;

public abstract class BarcodeQuestionInput extends LinearLayout {

    EditText barcodeEdit;
    ImageButton addButton;
    Button scanButton;
    View manualInputSeparator;

    @Nullable
    BarcodeEditListener barcodeEditListener;

    @Nullable
    AddButtonListener addButtonListener;

    public BarcodeQuestionInput(Context context) {
        this(context, null);
    }

    public BarcodeQuestionInput(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.barcode_question_input, this);
        this.barcodeEdit = (EditText) findViewById(R.id.barcode_input);
        this.addButton = (ImageButton) findViewById(R.id.barcode_add_btn);
        this.scanButton = (Button) findViewById(R.id.scan_btn);
        this.manualInputSeparator = findViewById(R.id.barcode_manual_input_separator);
    }

    void setBarcodeEditListener(BarcodeEditListener barcodeEditListener) {
        this.barcodeEditListener = barcodeEditListener;
    }

    void setScanButtonListener(@Nullable final ScanButtonListener scanButtonListener) {
        if (scanButton != null) {
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (scanButtonListener != null) {
                        scanButtonListener.onScanBarcodeTap();
                    }
                }
            });
        }
    }

    void setAddButtonListener(@Nullable AddButtonListener addButtonListener) {
        this.addButtonListener = addButtonListener;
    }

    void updateAddButton() {
        if (addButton != null) {
            addButton.setEnabled(!isBarcodeEmpty());
        }
    }

    boolean isBarcodeEmpty() {
        return TextUtils.isEmpty(getBarcode());
    }

    @NonNull
    String getBarcode() {
        return barcodeEdit.getText().toString();
    }

    abstract void setBarcodeText(String value);

    abstract void initViews();

    interface AddButtonListener {

        void onQuestionAddTap(String text);
    }

    interface BarcodeEditListener {

        void onTextEdited(String text);
    }

    static abstract class BarcodeEditWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //EMPTY
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //EMPTY
        }
    }
}
