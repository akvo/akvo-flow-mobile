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
import android.util.AttributeSet;

public class SingleLockedBarcodeQuestionInput extends LockedBarcodeQuestionInput {

    public SingleLockedBarcodeQuestionInput(Context context) {
        this(context, null);
    }

    public SingleLockedBarcodeQuestionInput(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    void initViews() {
        super.initViews();
        barcodeEdit.setVisibility(VISIBLE);
        barcodeEdit.setEnabled(false);
        barcodeEdit.setFocusable(false);
        barcodeEdit.setHint("");
    }

    @Override
    void setBarcodeText(String value) {
        barcodeEdit.setText(value);
    }
}
