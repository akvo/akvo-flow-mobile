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

import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyViewActivity;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.Question;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * this tab handles rendering of all validation errors so the user can see what
 * is is preventing submission of a survey.
 * 
 * @author Christopher Fagiani
 */
public class SubmitTabContentFactory extends SurveyTabContentFactory {
    private Button submitButton;

    public SubmitTabContentFactory(SurveyViewActivity c,
            SurveyDbAdapter dbAdaptor, float textSize, String defaultLang,
            String[] languageCodes) {
        super(c, dbAdaptor, textSize, defaultLang, languageCodes);
    }

    @Override
    public View createTabContent(String tag) {
        return replaceViewContent(null);
    }

    /**
     * checks completion status of questions and renders the view appropriately
     * 
     * @return
     */
    public View refreshView(boolean setMissing) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout ll = (LinearLayout)inflater.inflate(R.layout.submit_tab_view, null);

        LinearLayout mandatoryContainer = (LinearLayout)ll.findViewById(R.id.mandatory_container);
        View submitText = ll.findViewById(R.id.submit_text);

        // first, re-save all questions to make sure we didn't miss anything
        context.saveAllResponses();
        submitButton = configureActionButton(R.string.submitbutton,
                new OnClickListener() {
                    public void onClick(View v) {
                        context.saveSessionDuration();

                        // if we have no missing responses, submit the survey
                        databaseAdaptor.updateSurveyStatus(context.getRespondentId(),
                                SurveyInstanceStatus.SUBMITTED);
                        // send a broadcast message indicating new data is available
                        Intent i = new Intent(ConstantUtil.DATA_AVAILABLE_INTENT);
                        context.sendBroadcast(i);
                        
                        ViewUtil.showConfirmDialog(
                                R.string.submitcompletetitle, 
                                R.string.submitcompletetext, context, 
                                false,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (dialog != null) {
                                            context.finish();
                                        }
                                    }
                                });
                    }
                });

        // get the list (across all tabs) of missing mandatory responses
        // TODO: Do not request the invalid questions, and then set them, as it will loop through
        // TODO: every single question twice! Loop instead once, setting the errors and just returning
        // TODO: true/false if errors were found.
        ArrayList<Question> invalidQuestions = context.checkMandatory();
        if (setMissing) {
            context.setMissingQuestions(invalidQuestions);
        }
        if (invalidQuestions.size() == 0) {
            mandatoryContainer.setVisibility(View.GONE);
            submitText.setVisibility(View.VISIBLE);
            toggleButtons(true);
        } else {
            for (int i = 0; i < invalidQuestions.size(); i++) {
                QuestionView qv = new QuestionHeaderView(context, invalidQuestions.get(i),
                        getDefaultLang(), languageCodes, true);
                qv.suppressHelp(true);
                // force the view to be visible (if the question has
                // dependencies, it'll be hidden by default)
                qv.setVisibility(View.VISIBLE);
                View ruler = new View(context);
                ruler.setBackgroundColor(0xFFFFFFFF);
                mandatoryContainer.addView(qv);
                mandatoryContainer.addView(ruler, new LayoutParams(LayoutParams.MATCH_PARENT, 2));
            }

            mandatoryContainer.setVisibility(View.VISIBLE);
            submitText.setVisibility(View.GONE);
            toggleButtons(false);
        }

        ll.addView(submitButton);

        return replaceViewContent(ll);
    }

}
