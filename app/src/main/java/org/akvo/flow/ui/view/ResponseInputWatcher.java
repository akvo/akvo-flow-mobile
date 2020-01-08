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

package org.akvo.flow.ui.view;

import android.text.Editable;
import android.text.TextWatcher;

import java.lang.ref.WeakReference;

public class ResponseInputWatcher implements TextWatcher {

    private final WeakReference<QuestionView> questionViewWeakReference;

    private boolean ignoreEmptyInput = false;

    public ResponseInputWatcher(QuestionView questionViewWeakReference) {
        this.questionViewWeakReference = new WeakReference<>(questionViewWeakReference);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // EMPTY
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        ignoreEmptyInput = before == 0 && count == 0;
    }

    @Override
    public void afterTextChanged(Editable s) {
        QuestionView questionView = questionViewWeakReference.get();
        if (!ignoreEmptyInput && questionView != null) {
            questionView.captureResponse();
        }
    }
}