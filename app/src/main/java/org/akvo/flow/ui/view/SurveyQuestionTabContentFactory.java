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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyViewActivity;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.ResponseColumns;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.util.ConstantUtil;

/**
 * Creates the content for a single tab in the survey (corresponds to a
 * QuestionGroup). The tab will lay out all the questions in the QuestionGroup
 * (passed in at construction) in a List view and will append save/clear buttons
 * to the bottom of the list.
 * 
 * @author Christopher Fagiani
 */
public class SurveyQuestionTabContentFactory extends SurveyTabContentFactory {
    private QuestionGroup questionGroup;

    private HashMap<String, QuestionView> questionMap;
    private HashMap<String, QuestionResponse> responseMap;

    private Set<String> questionIds;
    private Map<String, String> sourceQuestionMap;

    private boolean readOnly;

    public HashMap<String, QuestionView> getQuestionMap() {
        return questionMap;
    }

    /**
     * stores the context and questionGroup to member fields
     * 
     * @param c
     * @param qg
     */
    public SurveyQuestionTabContentFactory(SurveyViewActivity c,
            QuestionGroup qg, SurveyDbAdapter dbAdaptor, float textSize,
            String defaultLang, String[] languageCodes, boolean readOnly) {
        super(c, dbAdaptor, textSize, defaultLang, languageCodes);
        responseMap = null;
        questionGroup = qg;
        questionMap = new HashMap<String, QuestionView>();
        questionIds = new HashSet<String>();
        sourceQuestionMap = new HashMap<String, String>();
        this.readOnly = readOnly;

        for (Question q : questionGroup.getQuestions()) {
            questionIds.add(q.getId());
            if (!TextUtils.isEmpty(q.getSourceQuestionId())) {
                // If this question has a source question id, add it to the map
                sourceQuestionMap.put(q.getSourceQuestionId(), q.getId());
            }
        }
    }

    /**
     * Constructs a view using the question data from the stored questionGroup.
     * This method makes use of a QuestionAdaptor to process individual
     * questions.
     */
    public View createTabContent(String tag) {
        ScrollView scrollView = createSurveyTabContent();

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        scrollView.addView(ll);

        ArrayList<Question> questions = questionGroup.getQuestions();

        for (int i = 0; i < questions.size(); i++) {
            QuestionView questionView = null;
            Question q = questions.get(i);

            if (ConstantUtil.OPTION_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new OptionQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);

            } else if (ConstantUtil.FREE_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new FreetextQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
            } else if (ConstantUtil.PHOTO_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new MediaQuestionView(context, q,
                        ConstantUtil.PHOTO_QUESTION_TYPE, getDefaultLang(),
                        languageCodes, readOnly);
            } else if (ConstantUtil.VIDEO_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new MediaQuestionView(context, q,
                        ConstantUtil.VIDEO_QUESTION_TYPE, getDefaultLang(),
                        languageCodes, readOnly);
            } else if (ConstantUtil.GEO_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new GeoQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
            } else if (ConstantUtil.SCAN_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new BarcodeQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
            } else if (ConstantUtil.STRENGTH_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                /*
                questionView = new StrengthQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
                        */
            } else if (ConstantUtil.HEADING_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                /*
                questionView = new CompassQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
                        */
            } else if (ConstantUtil.DATE_QUESTION_TYPE.equalsIgnoreCase(q
                    .getType())) {
                questionView = new DateQuestionView(context, q,
                        getDefaultLang(), languageCodes, readOnly);
            } else {
                // TODO: The base class should *NOT* be instantiated!
                questionView = new QuestionView(context, q, getDefaultLang(),
                        languageCodes, readOnly);
            }
            questionView.setTextSize(defaultTextSize);
            questionMap.put(q.getId(), questionView);
            questionView.addQuestionInteractionListener((SurveyViewActivity) context);
            ll.addView(questionView);
            if (i < questions.size() - 1) {
                View ruler = new View(context);
                ruler.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                ruler.setBackgroundColor(0xFFFFFFFF);
                questionView.addView(ruler);
            }
        }
        // set up listeners for dependencies. Since the dependencies can span
        // groups, the parent needs to do this
        context.establishDependencies(questionGroup);

        // create save/clear buttons
        Button nextButton = configureActionButton(R.string.nextbutton,
                new OnClickListener() {
                    public void onClick(View v) {
                        context.advanceTab();
                    }
                });
        if (readOnly) {
            toggleButtons(false);
        }

        ll.addView(nextButton);

        loadState(context.getRespondentId());
        return scrollView;
    }

    /**
     * resets all the questions on this tab
     */
    public void resetTabQuestions() {
        // while in general we avoid the enhanced for-loop in
        // the Android VM, we can use it here because we
        // would still need the iterator
        if (questionMap != null) {
            for (QuestionView view : questionMap.values()) {
                view.resetQuestion(false);
            }
            resetView();
        }
        if (responseMap != null) {
            responseMap.clear();
        }
    }

    /**
     * sets the mandatory highlight on each question that is in the set of
     * question ids passed in
     * 
     * @param questions
     */
    public void highlightMissingQuestions(HashSet<String> questions) {
        if (questionMap != null && questions != null) {
            for (QuestionView view : questionMap.values()) {
                if (questions.contains(view.getQuestion().getId())) {
                    view.highlight(true);
                } else {
                    view.highlight(false);
                }
            }
        }
    }

    /**
     * updates the visible languages for all questions in the tab
     * 
     * @param langCodes
     */
    public void updateQuestionLanguages(String[] langCodes) {
        updateSelectedLanguages(langCodes);
        if (questionMap != null) {
            for (QuestionView view : questionMap.values()) {
                view.updateSelectedLanguages(langCodes);
            }
        }
    }

    /**
     * checks to make sure the mandatory questions in this tab have a response
     * 
     * @return
     */
    public ArrayList<Question> checkMandatoryQuestions() {
        if (responseMap == null) {
            loadState(context.getRespondentId());
        }
        ArrayList<Question> missingQuestions = new ArrayList<Question>();
        // we have to check if the map is null or empty since the views aren't
        // created until the tab is clicked the first time
        if (questionMap == null || questionMap.size() == 0) {
            // add all the mandatory questions
            ArrayList<Question> uninitializedQuesitons = questionGroup
                    .getQuestions();
            for (int i = 0; i < uninitializedQuesitons.size(); i++) {
                if (uninitializedQuesitons.get(i).isMandatory()) {
                    QuestionResponse resp = responseMap
                            .get(uninitializedQuesitons.get(i).getId());
                    if (resp == null || !resp.isValid()) {
                        missingQuestions.add(uninitializedQuesitons.get(i));
                    }
                }
            }
        } else {
            for (QuestionView view : questionMap.values()) {
                if (view.getQuestion().isMandatory()) {
                    QuestionResponse resp = view.getResponse();
                    if (resp == null || !resp.isValid()) {
                        missingQuestions.add(view.getQuestion());
                    }
                }
            }
        }
        return missingQuestions;
    }
    
    public HashMap<String, QuestionResponse> loadState(Long respondentId) {
        return loadState(respondentId, false);
    }

    /**
     * loads the state from the database using the respondentId passed in. It
     * will then use the loaded responses to update the status of the question
     * views in this tab.
     * 
     * @param respondentId
     */
    public HashMap<String, QuestionResponse> loadState(Long respondentId,
            boolean prefill) {
        if (responseMap == null) {
            responseMap = new HashMap<String, QuestionResponse>();
        }
        if (respondentId != null) {
            Cursor responseCursor = databaseAdaptor.getResponses(respondentId);

            while (responseCursor.moveToNext()) {
                String[] cols = responseCursor.getColumnNames();
                QuestionResponse resp = new QuestionResponse();
                // TODO: Use cursor.get.... functions instead of looping through the results
                for (int i = 0; i < cols.length; i++) {
                    if (cols[i].equals(ResponseColumns._ID)) {
                        resp.setId(responseCursor.getLong(i));
                    } else if (cols[i].equals(ResponseColumns.SURVEY_INSTANCE_ID)) {
                        resp.setRespondentId(responseCursor.getLong(i));
                    } else if (cols[i].equals(ResponseColumns.ANSWER)) {
                        resp.setValue(responseCursor.getString(i));
                    } else if (cols[i].equals(ResponseColumns.TYPE)) {
                        resp.setType(responseCursor.getString(i));
                    } else if (cols[i].equals(ResponseColumns.QUESTION_ID)) {
                        resp.setQuestionId(responseCursor.getString(i));
                    } else if (cols[i].equals(ResponseColumns.INCLUDE)) {
                        resp.setIncludeFlag(responseCursor.getInt(i) == 1);
                    } else if (cols[i].equals(ResponseColumns.SCORED_VAL)) {
                        resp.setScoredValue(responseCursor.getString(i));
                    } else if (cols[i].equals(ResponseColumns.STRENGTH)) {
                        resp.setStrength(responseCursor.getString(i));
                    }
                }
                
                String questionId = resp.getQuestionId();

                if (!questionIds.contains(questionId) && sourceQuestionMap.get(questionId) != null) {
                    questionId = sourceQuestionMap.get(questionId);
                    resp.setQuestionId(questionId);// Update the value with the current questionId
                }

                // If the questionId is not part of questionIds, skip this response
                if (!questionIds.contains(questionId)) {
                    continue;
                }

                if (prefill) {
                    // Copying values from old instance; Get rid of its Id
                    // Also, update the respondentId, matching the current one
                    resp.setId(null);
                    resp.setRespondentId(context.getRespondentId());

                    databaseAdaptor.createOrUpdateSurveyResponse(resp);
                }

                responseMap.put(questionId, resp);

                if (questionMap.get(questionId) != null) {
                    // Update the question view to reflect the loaded data
                    questionMap.get(questionId).rehydrate(resp);
                }
            }
            responseCursor.close();
        }
        return responseMap;
    }

    /**
     * updates text size of all questions in this tab
     * 
     * @param size
     */
    public void updateTextSize(float size) {
        defaultTextSize = size;
        if (questionMap != null) {
            for (QuestionView qv : questionMap.values()) {
                qv.setTextSize(size);
            }
        }
    }

    /**
     * persists the current question responses in this tab to the database
     * 
     * @param respondentId
     */
    public void saveState(Long respondentId) {
        if (responseMap == null) {
            responseMap = new HashMap<String, QuestionResponse>();
        }
        if (questionMap != null) {
            for (QuestionView q : questionMap.values()) {
                QuestionResponse curResponse = q.getResponse(true);
                if (curResponse != null
                        && curResponse.hasValue()) {
                    curResponse.setRespondentId(respondentId);
                    databaseAdaptor.createOrUpdateSurveyResponse(curResponse);
                    responseMap.put(curResponse.getQuestionId(),
                            curResponse);
                } else if (curResponse != null
                        && curResponse.getId() != null
                        && curResponse.getId() > 0) {
                    // if we don't have a value BUT there is an ID, we need to
                    // remove it since the user blanked out their response
                    databaseAdaptor.deleteResponse(respondentId.toString(), q
                            .getQuestion().getId());
                    responseMap.remove(curResponse.getQuestionId());
                } else if (curResponse != null) {
                    // if we're here, the response is blank but hasn't been
                    // saved yet (has no ID)
                    // so we can just discard it
                    responseMap.remove(curResponse.getQuestionId());
                }

                // Notify the View so it can release any system resource (i.e.
                // Location updates)
                q.releaseResources();
            }
        }
    }
}
