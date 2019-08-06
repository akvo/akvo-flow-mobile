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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionView;

import java.util.ArrayList;

public class BarcodeQuestionViewReadOnly extends QuestionView {

    private BarcodeQuestionAdapter barcodeQuestionAdapter;

    public BarcodeQuestionViewReadOnly(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view_multiple);
        RecyclerView responses = (RecyclerView) findViewById(R.id.barcode_responses_recycler_view);
        responses.setLayoutManager(new LinearLayoutManager(getContext()));
        barcodeQuestionAdapter = new BarcodeQuestionAdapter(new ArrayList<String>(), null);
        responses.setAdapter(barcodeQuestionAdapter);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String answer = resp != null ? resp.getValue() : null;
        if (!TextUtils.isEmpty(answer)) {
            String[] values = answer.split("\\|", -1);
            barcodeQuestionAdapter.addBarcodes(values);
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        //EMPTY
    }
}
