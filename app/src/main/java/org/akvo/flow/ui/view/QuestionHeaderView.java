/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.ui.view;

import android.content.Context;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;

public class QuestionHeaderView extends QuestionView {

    public QuestionHeaderView(Context context, Question q, String defaultLang,
                              String[] langCodes, boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        init();
    }

    private void init() {
        setQuestionView(R.layout.question_header);
    }

}
