/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.ui.view.cascade;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import org.akvo.flow.R;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.CascadeNode;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CascadeQuestionView extends QuestionView implements CascadeView {

    public  static final int POSITION_NONE = -1; // no textView position id
    private static final long ID_NONE = -1; // no node id
    private static final long ID_ROOT = 0; // root node id

    private String[] mLevels;
    private LinearLayout cascadeLevelsContainer;
    private boolean mFinished;

    @Inject
    CascadePresenter presenter;

    public CascadeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.cascade_question_view);
        initialiseInjector();

        cascadeLevelsContainer = findViewById(R.id.cascade_content);

        // Load level names
        List<Level> levels = getQuestion().getLevels();
        if (levels != null) {
            mLevels = new String[levels.size()];
            for (int i = 0; i < levels.size(); i++) {
                mLevels[i] = levels.get(i).getText();
            }
        }
        presenter.setView(this);
        presenter.loadCascadeData(getQuestion().getSrc(), getContext());
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @Override
    public void onDestroy() {
       presenter.destroy();
    }

    public void updateTextViews(int updatedSpinnerIndex) {
        if (!presenter.isValidDatabase()) {
            return;
        }

        final int nextLevel = updatedSpinnerIndex + 1;

        // First, clean up descendant textViews (if any)
        while (nextLevel < cascadeLevelsContainer.getChildCount()) {
            cascadeLevelsContainer.removeViewAt(nextLevel);
        }

        long parent = ID_ROOT;
        if (updatedSpinnerIndex != POSITION_NONE) {
            Node node = getSelectedNode(updatedSpinnerIndex);
            if (node == null || node.getId() == ID_NONE) {
                // if this is the first level, it means we've got no answer at all
                mFinished = false;
                return; // Do not load more levels
            } else {
                parent = node.getId();
            }
        }

        List<Node> values = presenter.loadValuesForParent(parent);
        if (!values.isEmpty()) {
            addLevelView(nextLevel, values, POSITION_NONE);
            mFinished = updatedSpinnerIndex == POSITION_NONE;
        } else {
            mFinished = true;// no more levels
        }
    }

    private Node getSelectedNode(int textViewIndex) {
        View childAt = cascadeLevelsContainer.getChildAt(textViewIndex);
        AutoCompleteTextView textView = childAt != null? childAt.findViewById(R.id.cascade_level_textview): null;
        return textView != null? ((CascadeAdapter) textView.getAdapter()).getItem(textView.getText().toString()): null;
    }

    private void addLevelView(int position, List<Node> values, int selection) {
        Timber.d("Will add nodes to " + position + ", values: " + values + ", selection: " + selection);
        if (values.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.cascading_level_item, cascadeLevelsContainer, false);
        final TextView text = view.findViewById(R.id.cascade_level_number);
        final FlowAutoComplete autoCompleteTextView = view.findViewById(R.id.cascade_level_textview);
        final TextInputLayout layout = view.findViewById(R.id.outlinedTextField);

        String levelTitle = mLevels != null && mLevels.length > position ? mLevels[position] : "";
        text.setText(levelTitle);

        layout.setHint(getContext().getString(R.string.cascade_level_textview_hint, levelTitle));
        layout.setTag(position);

        autoCompleteTextView.updateAutoComplete(position, values, selection, isReadOnly());
        autoCompleteTextView.setOnItemClickListener((parent, view1, position1, id) -> {
            int index = (int) autoCompleteTextView.getTag();
            updateTextViews(index);
            captureResponse();
            layout.setError(null);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            autoCompleteTextView.setOnDismissListener(() -> {
                if (autoCompleteTextView.getSelectedItem() == null) {
                    layout.setError(getContext().getString(R.string.cascade_level_textview_error));
                    int index = (int) autoCompleteTextView.getTag();
                    updateTextViews(index);
                }
            });
        }
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!autoCompleteTextView.isPopupShowing()) {
                    int index = (int) autoCompleteTextView.getTag();
                    updateTextViews(index);
                    captureResponse();
                    if (autoCompleteTextView.getSelectedItem() == null) {
                        layout.setError(getContext().getString(R.string.cascade_level_textview_error));
                        updateTextViews(index);
                    } else {
                        layout.setError(null);
                    }
                } else {
                    layout.setError(null);
                }
            }
        });
        cascadeLevelsContainer.addView(view);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        cascadeLevelsContainer.removeAllViews();
        String answer = resp != null ? resp.getValue() : null;
        if (!presenter.isValidDatabase() || TextUtils.isEmpty(answer)) {
            return;
        }

        List<CascadeNode> values = CascadeValue.deserialize(answer);

        // For each existing value, we load the corresponding level nodes, and create an
        // AutocompleteTextView automatically selecting the token. On each iteration, we keep track of selected
        // value's id, in order to fetch the descendant nodes from the DB.
        int index = 0;
        long parentId = 0;
        while (index < values.size()) {
            int valuePosition = POSITION_NONE;
            List<Node> cascadeLevelValues = presenter.loadValuesForParent(parentId);
            for (int pos = 0; pos < cascadeLevelValues.size(); pos++) {
                Node node = cascadeLevelValues.get(pos);
                CascadeNode v = values.get(index);
                if (node.getName().equals(v.getName())) {
                    valuePosition = pos;
                    parentId = node.getId();
                    break;
                }
            }

            if (valuePosition == POSITION_NONE || cascadeLevelValues.isEmpty()) {
                cascadeLevelsContainer.removeAllViews();
                return;// Cannot reassemble response
            }
            addLevelView(index, cascadeLevelValues, valuePosition);
            index++;
        }
        if (!isReadOnly()) {
            updateTextViews(index - 1);// Last updated item position
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        if (isReadOnly()) {
            cascadeLevelsContainer.removeAllViews();
        } else {
            updateTextViews(POSITION_NONE);
        }
        if (!presenter.isValidDatabase()) {
            String error = getContext()
                    .getString(R.string.cascade_error_message, getQuestion().getSrc());
            Timber.e(new IllegalStateException(error));
            setError(error);
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        List<CascadeNode> values = new ArrayList<>();
        for (int i = 0; i < cascadeLevelsContainer.getChildCount(); i++) {
            Node node = getSelectedNode(i);
            if (node != null && node.getId() != ID_NONE) {
                CascadeNode v = new CascadeNode();
                v.setName(node.getName());
                v.setCode(node.getCode());
                values.add(v);
            }
        }

        String response = CascadeValue.serialize(values);
        Question question = getQuestion();
        setResponse(suppressListeners, question, response, ConstantUtil.CASCADE_RESPONSE_TYPE);
    }

    @Override
    public boolean isValid() {
        boolean valid = super.isValid() && mFinished;
        if (!valid) {
            setError(getResources().getString(R.string.error_question_mandatory));
        }
        return valid;
    }

}
