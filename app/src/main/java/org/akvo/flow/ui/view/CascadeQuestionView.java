/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CascadeQuestionView extends QuestionView implements AdapterView.OnItemSelectedListener {
    private static final int POSITION_NONE = -1;// no spinner position id

    private static final long ID_NONE = -1;// no node id
    private static final long ID_ROOT = 0;// root node id

    private String[] mLevels;
    private LinearLayout mSpinnerContainer;
    private TextView mAnswer;

    private CascadeDB mDatabase;

    public CascadeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.cascade_question_view);

        mSpinnerContainer = (LinearLayout)findViewById(R.id.cascade_content);
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
        final File db = new File(FileUtil.getFilesDir(FileType.RES), src);
        if (!db.exists()) {
            throw new IllegalStateException("Cascade resource file doesn't exist at "
                    + db.getAbsolutePath());
        }

        mDatabase = new CascadeDB(getContext(), db.getAbsolutePath());
        mDatabase.open();
        //update(POSITION_NONE);
    }

    private void update(int updatedSpinnerIndex) {
        final int nextLevel = updatedSpinnerIndex + 1;

        // First, clean up descendant spinners (if any)
        while (nextLevel < mSpinnerContainer.getChildCount()) {
            mSpinnerContainer.removeViewAt(nextLevel);
        }

        long parent = ID_ROOT;
        if (updatedSpinnerIndex != POSITION_NONE) {
            Node node = (Node)getSpinner(updatedSpinnerIndex).getSelectedItem();
            if (node.getId() == ID_NONE) {
                return; // Do not load more levels
            } else {
                parent = node.getId();
            }
        }

        final Spinner spinner = createSpinner(nextLevel, mDatabase.getValues(parent));
        if (spinner != null) {
            mSpinnerContainer.addView(spinner);
        }
    }

    private Spinner createSpinner(int position, List<Node> values) {
        if (values.isEmpty()) {
            return null;
        }

        final Spinner spinner = new Spinner(getContext());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        // Insert a fake value with the title
        String value = mLevels != null && mLevels.length >= position ? mLevels[position] : "";
        Node node = new Node(ID_NONE, value);
        values.add(0, node);

        ArrayAdapter<Node> adapter = new ArrayAdapter<Node>(getContext(),
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setTag(position);
        spinner.setAdapter(adapter);
        spinner.setEnabled(!isReadOnly());
        // Attach listener asynchronously, preventing selection event from being fired off right away
        spinner.post(new Runnable() {
            public void run() {
                spinner.setOnItemSelectedListener(CascadeQuestionView.this);
            }
        });
        return spinner;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int index = (Integer)parent.getTag();
        update(index);
        captureResponse();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        final int index = (Integer)parent.getTag();
        update(index);
        captureResponse();// TODO: Is this needed?
    }

    @Override
    public void releaseResources() {
        mDatabase.close();
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String answer = resp != null ? resp.getValue() : null;
        if (TextUtils.isEmpty(answer)) {
            return;
        }
        mSpinnerContainer.removeAllViews();
        String[] values = answer.split("\\|", -1);

        int index = 0;
        long parentId = 0;
        while (index < values.length) {
            int valuePosition = POSITION_NONE;
            List<Node> spinnerValues = mDatabase.getValues(parentId);
            for (int pos=0; pos<spinnerValues.size(); pos++) {
                Node node = spinnerValues.get(pos);
                if (node.getValue().equals(values[index])) {
                    valuePosition = pos;
                    parentId = node.getId();
                    break;
                }
            }

            Spinner spinner = createSpinner(index, spinnerValues);

            if (valuePosition == POSITION_NONE || spinner == null) {
                return;// Cannot reassemble response
            }
            spinner.setSelection(valuePosition+1);// Skip level title item
            mSpinnerContainer.addView(spinner);
            index++;
        }
        update(index-1);// Last updated item position
        mAnswer.setText(answer);
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        update(POSITION_NONE);
        mAnswer.setText("");
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        // For the path we've got so far
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<mSpinnerContainer.getChildCount(); i++) {
            Node node = (Node)getSpinner(i).getSelectedItem();
            if (node.getId() != ID_NONE) {
                builder.append("|").append(node.toString());
            }
        }
        // Skip the first "|", if found.
        String response = builder.length() > 0 ? builder.substring(1) : "";
        mAnswer.setText(response);// tmp visualization of the response -- will go away

        setResponse(new QuestionResponse(response, ConstantUtil.VALUE_RESPONSE_TYPE,
                getQuestion().getId()), suppressListeners);
    }

    private Spinner getSpinner(int position) {
        return (Spinner)mSpinnerContainer.getChildAt(position);
    }
}
