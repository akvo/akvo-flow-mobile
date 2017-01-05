/*
 *  Copyright (C) 2014-2017 Stichting Akvo (Akvo Foundation)
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
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.data.CascadeDB;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.CascadeNode;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CascadeQuestionView extends QuestionView
        implements AdapterView.OnItemSelectedListener {
    private static final String TAG = CascadeQuestionView.class.getSimpleName();
    private static final int POSITION_NONE = -1;// no spinner position id

    private static final long ID_NONE = -1;// no node id
    private static final long ID_ROOT = 0;// root node id

    private String[] mLevels;
    private LinearLayout mSpinnerContainer;
    private boolean mFinished;

    private CascadeDB mDatabase;

    public CascadeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.cascade_question_view);

        mSpinnerContainer = (LinearLayout) findViewById(R.id.cascade_content);

        // Load level names
        List<Level> levels = getQuestion().getLevels();
        if (levels != null) {
            mLevels = new String[levels.size()];
            for (int i = 0; i < levels.size(); i++) {
                mLevels[i] = levels.get(i).getText();
            }
        }

        // Construct local filename (src refers to remote location of the resource)
        String src = getQuestion().getSrc();
        if (!TextUtils.isEmpty(src)) {
            File db = new File(FileUtil.getFilesDir(FileType.RES), src);
            if (db.exists()) {
                mDatabase = new CascadeDB(getContext(), db.getAbsolutePath());
                mDatabase.open();
            }
        }
        updateSpinners(POSITION_NONE);
    }

    @Override
    public void onResume() {
        if (mDatabase != null && !mDatabase.isOpen()) {
            mDatabase.open();
        }
    }

    @Override
    public void onPause() {
        if (mDatabase != null) {
            mDatabase.close();
        }
    }

    private void updateSpinners(int updatedSpinnerIndex) {
        if (mDatabase == null) {
            return;
        }

        final int nextLevel = updatedSpinnerIndex + 1;

        // First, clean up descendant spinners (if any)
        while (nextLevel < mSpinnerContainer.getChildCount()) {
            mSpinnerContainer.removeViewAt(nextLevel);
        }

        long parent = ID_ROOT;
        if (updatedSpinnerIndex != POSITION_NONE) {
            Node node = (Node) getSpinner(updatedSpinnerIndex).getSelectedItem();
            if (node.getId() == ID_NONE) {
                // if this is the first level, it means we've got no answer at all
                mFinished = false;
                return; // Do not load more levels
            } else {
                parent = node.getId();
            }
        }

        List<Node> values = mDatabase.getValues(parent);
        if (!values.isEmpty()) {
            addLevelView(nextLevel, values, POSITION_NONE);
            mFinished = updatedSpinnerIndex == POSITION_NONE;
        } else {
            mFinished = true;// no more levels
        }
    }

    private void addLevelView(int position, List<Node> values, int selection) {
        if (values.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.cascading_level_item, mSpinnerContainer, false);
        final TextView text = (TextView) view.findViewById(R.id.text);
        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

        text.setText(mLevels != null && mLevels.length > position ? mLevels[position] : "");

        // Insert a fake 'Select' value
        Node node = new Node(ID_NONE, getContext().getString(R.string.select), null);
        values.add(0, node);

        SpinnerAdapter adapter = new CascadeAdapter(getContext(), values);
        spinner.setAdapter(adapter);
        spinner.setTag(position);// Tag the spinner with its position within the container
        spinner.setEnabled(!isReadOnly());
        if (selection != POSITION_NONE) {
            spinner.setSelection(selection + 1);// Skip level title item
        }
        // Attach listener asynchronously, preventing selection event from being fired off right away
        spinner.post(new Runnable() {
            public void run() {
                spinner.setOnItemSelectedListener(CascadeQuestionView.this);
            }
        });
        mSpinnerContainer.addView(view);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final int index = (Integer) parent.getTag();
        updateSpinners(index);
        captureResponse();
        setError(null);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        String answer = resp != null ? resp.getValue() : null;
        if (mDatabase == null || TextUtils.isEmpty(answer)) {
            return;
        }
        mSpinnerContainer.removeAllViews();

        List<CascadeNode> values = CascadeValue.deserialize(answer);

        // For each existing value, we load the corresponding level nodes, and create a spinner
        // view, automatically selecting the token. On each iteration, we keep track of selected
        // value's id, in order to fetch the descendant nodes from the DB.
        int index = 0;
        long parentId = 0;
        while (index < values.size()) {
            int valuePosition = POSITION_NONE;
            List<Node> spinnerValues = mDatabase.getValues(parentId);
            for (int pos = 0; pos < spinnerValues.size(); pos++) {
                Node node = spinnerValues.get(pos);
                CascadeNode v = values.get(index);
                if (node.getName().equals(v.getName())) {
                    valuePosition = pos;
                    parentId = node.getId();
                    break;
                }
            }

            if (valuePosition == POSITION_NONE || spinnerValues.isEmpty()) {
                mSpinnerContainer.removeAllViews();
                return;// Cannot reassemble response
            }
            addLevelView(index, spinnerValues, valuePosition);
            index++;
        }
        if (!isReadOnly()) {
            updateSpinners(index - 1);// Last updated item position
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        updateSpinners(POSITION_NONE);
        if (mDatabase == null) {
            String error = "Cannot load cascade resource: " + getQuestion().getSrc();
            Log.e(TAG, error);
            PersistentUncaughtExceptionHandler.recordException(new IllegalStateException(error));
            setError(error);
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        List<CascadeNode> values = new ArrayList<>();
        for (int i = 0; i < mSpinnerContainer.getChildCount(); i++) {
            Node node = (Node) getSpinner(i).getSelectedItem();
            if (node.getId() != ID_NONE) {
                CascadeNode v = new CascadeNode();
                v.setName(node.getName());
                v.setCode(node.getCode());
                values.add(v);
            }
        }

        String response = CascadeValue.serialize(values);
        setResponse(new QuestionResponse(response, ConstantUtil.CASCADE_RESPONSE_TYPE,
                getQuestion().getId()), suppressListeners);
    }

    private Spinner getSpinner(int position) {
        return (Spinner) mSpinnerContainer.getChildAt(position).findViewById(R.id.spinner);
    }

    @Override
    public boolean isValid() {
        boolean valid = super.isValid() && mFinished;
        if (!valid) {
            setError(getResources().getString(R.string.error_question_mandatory));
        }
        return valid;
    }

    private static class CascadeAdapter extends ArrayAdapter<Node> {

        CascadeAdapter(Context context, List<Node> objects) {
            super(context, R.layout.cascade_spinner_item, R.id.cascade_spinner_item_text, objects);
            setDropDownViewResource(R.layout.cascade_spinner_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            setStyle(view, position);
            return view;
        }

        @Override
        public View getDropDownView(final int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            setStyle(view, position);
            return view;
        }

        private void setStyle(View view, int position) {
            try {
                TextView text = (TextView) view.findViewById(R.id.cascade_spinner_item_text);
                int flags = text.getPaintFlags();
                if (position == 0) {
                    flags |= Paint.FAKE_BOLD_TEXT_FLAG;
                } else {
                    flags &= ~Paint.FAKE_BOLD_TEXT_FLAG;
                }
                text.setPaintFlags(flags);
            } catch (ClassCastException e) {
                Log.e("CascadeAdapter", "View cannot be casted to TextView!");
            }
        }
    }
}
