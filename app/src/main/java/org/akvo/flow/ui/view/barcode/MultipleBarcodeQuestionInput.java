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
import androidx.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;

public class MultipleBarcodeQuestionInput extends BarcodeQuestionInput {

    public MultipleBarcodeQuestionInput(Context context) {
        this(context, null);
    }

    public MultipleBarcodeQuestionInput(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    void initViews() {
        barcodeEdit.setVisibility(VISIBLE);
        manualInputSeparator.setVisibility(VISIBLE);
        addButton.setVisibility(VISIBLE);
        scanButton.setVisibility(VISIBLE);
        barcodeEdit.addTextChangedListener(new BarcodeEditWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateAddButton();
            }
        });
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addButtonListener != null) {
                    addButtonListener.onQuestionAddTap(getBarcode());
                }
            }
        });
    }

    @Override
    void setBarcodeText(String value) {
        barcodeEdit.setText(value);
    }
}
