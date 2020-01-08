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

package org.akvo.flow.ui.view.option;

import android.content.Context;

import org.akvo.flow.R;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;

import java.util.Collections;
import java.util.List;

public class OptionQuestionViewNull extends OptionQuestionView {

    public OptionQuestionViewNull(Context context, Question q,
            SurveyListener surveyListener) {
        super(context, q, surveyListener);
    }

    @Override
    void init() {
        setQuestionView(R.layout.question_header);
    }

    @Override
    void initOptionViews() {
        //EMPTY
    }

    @Override
    List<Option> getSelection() {
        return Collections.EMPTY_LIST;
    }

    @Override
    void checkMatchingItem(int i) {
        //EMPTY
    }

    @Override
    void resetViews() {
        //EMPTY
    }
}
