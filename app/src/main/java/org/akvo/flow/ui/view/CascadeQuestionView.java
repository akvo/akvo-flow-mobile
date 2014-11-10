/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.dao.CascadeDB;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;

import java.io.File;
import java.util.List;

public class CascadeQuestionView extends QuestionView {
    private String[] mLevels;

    private LinearLayout mContent;
    private TextView mAnswer;

    private CascadeDB mDatabase;

    public CascadeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.cascade_question_view);

        mContent = (LinearLayout)findViewById(R.id.cascade_content);
        mAnswer = (TextView)findViewById(R.id.answer);

        // Load level names
        List<Level> levels = getQuestion().getLevels();
        if (levels != null) {
            mLevels = new String[levels.size()];
            for (int i=0; i<levels.size(); i++) {
                mLevels[i] = levels.get(i).getText();
            }
        }

        // Construct local filename (src refers to remote location of the resource)
        String src = getQuestion().getSrc();
        if (TextUtils.isEmpty(src)) {
            throw new IllegalStateException("Cascade question must have a valid src");
        }

        // TODO: We need to determine whether src contains the URL or just the filename of the resource
        mDatabase = new CascadeDB(getContext(), new File(FileUtil.getFilesDir(FileType.RES), src).getAbsolutePath());
        mDatabase.open();
    }

    @Override
    public void releaseResources() {
        mDatabase.close();
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        // TODO:
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        // TODO:
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        // TODO:
    }

}
