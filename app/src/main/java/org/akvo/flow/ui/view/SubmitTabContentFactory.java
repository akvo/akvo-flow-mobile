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
import java.util.List;

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
        Button submitButton = configureActionButton(R.string.submitbutton,
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

        List<Question> invalidQuestions = new ArrayList<Question>();
        if (setMissing) {
            // Checking questions will force them to display any error status they contain.
            // Missing mandatory questions will also be marked as invalid.
            // Non-populated questions will only check for void mandatory responses.
            invalidQuestions = context.checkInvalidQuestions();
        }
        if (invalidQuestions.isEmpty()) {
            mandatoryContainer.setVisibility(View.GONE);
            submitText.setVisibility(View.VISIBLE);
            toggleButtons(true);
        } else {
            for (Question q : invalidQuestions) {
                QuestionView qv = new QuestionHeaderView(context, q, getDefaultLang(),
                        languageCodes, true);
                qv.suppressHelp(true);
                // force the view to be visible (if the question has
                // dependencies, it'll be hidden by default)
                qv.setVisibility(View.VISIBLE);
                View ruler = new View(context);
                ruler.setBackgroundColor(0xFFFFFFFF);// TODO: Externalize this color resource
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
