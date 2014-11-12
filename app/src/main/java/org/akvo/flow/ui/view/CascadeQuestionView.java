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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.dao.CascadeDB;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CascadeQuestionView extends QuestionView implements AdapterView.OnItemSelectedListener {
    private static final long DEFAULT_VALUE = -1;
    private static final int POSITION_NONE = -1;

    private String[] mLevels;
    private LinearLayout mSpinnerContainer;
    private TextView mAnswer;
    private List<Spinner> mSpinners;

    private CascadeDB mDatabase;

    public CascadeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.cascade_question_view);

        mSpinnerContainer = (LinearLayout)findViewById(R.id.cascade_content);
        mAnswer = (TextView)findViewById(R.id.answer);

        mSpinners = new ArrayList<Spinner>();

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
        final File db = new File(FileUtil.getFilesDir(FileType.RES), src);
        if (!db.exists()) {
            throw new IllegalStateException("Cascade resource file doesn't exist at "
                    + db.getAbsolutePath());
        }

        mDatabase = new CascadeDB(getContext(), db.getAbsolutePath());
        mDatabase.open();
        update(POSITION_NONE);
    }

    private void update(int updatedSpinnerIndex) {
        final int nextLevel = updatedSpinnerIndex + 1;

        // First, clean up descendant spinners (if any)
        while (nextLevel < mSpinners.size()) {
            mSpinners.remove(nextLevel);
            mSpinnerContainer.removeViewAt(nextLevel);
        }

        // For the path we've got so far
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= updatedSpinnerIndex; i++) {
            Node node = (Node)mSpinners.get(i).getSelectedItem();
            if (node.getId() != DEFAULT_VALUE) {
                builder.append(mSpinners.get(i).getSelectedItem().toString());
                if (i < updatedSpinnerIndex) {
                    builder.append(",");
                }
            }
        }
        mAnswer.setText(builder.toString());

        long parent = 0;
        if (updatedSpinnerIndex >= 0) {
            Node node = (Node)mSpinners.get(updatedSpinnerIndex).getSelectedItem();
            if (node.getId() == DEFAULT_VALUE) {
                return; // Do not load more levels
            } else {
                parent = node.getId();
            }
        }

        // Get next level values
        List<Node> values = mDatabase.getValues(parent);

        if (!values.isEmpty()) {
            Spinner spinner = new Spinner(getContext());
            spinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));

            // Insert a fake value with the title
            String value = mLevels != null && mLevels.length >= nextLevel ? mLevels[nextLevel]
                    : "Select Level " + (nextLevel);
            Node node = new Node(DEFAULT_VALUE, value);
            values.add(0, node);

            ArrayAdapter<Node> adapter = new ArrayAdapter<Node>(getContext(),
                    android.R.layout.simple_spinner_item, values);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setTag(nextLevel);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);

            mSpinnerContainer.addView(spinner);
            mSpinners.add(spinner);
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int index = (Integer)parent.getTag();
        update(index);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        final int index = (Integer)parent.getTag();
        update(index);
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
