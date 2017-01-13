/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import org.akvo.flow.R;
import org.akvo.flow.domain.AltText;
import org.akvo.flow.domain.Dependency;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionHelp;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class QuestionView extends LinearLayout implements QuestionInteractionListener {
    private static final int PADDING_DIP = 8;
    protected static String[] sColors = null;

    protected Question mQuestion;
    private QuestionResponse mResponse;

    private List<QuestionInteractionListener> mListeners;
    protected SurveyListener mSurveyListener;

    private TextView mQuestionText;
    private ImageButton mTipImage;

    /**
     * mError stores the presence of non-acceptable responses.
     * Any non-null value will be considered as an invalid response.
     */
    private String mError;

    public QuestionView(final Context context, Question q, SurveyListener surveyListener) {
        super(context);
        setOrientation(VERTICAL);
        final int padding = (int) PlatformUtil.dp2Pixel(getContext(), PADDING_DIP);
        setPadding(padding, padding, padding, padding);
        if (sColors == null) {
            // must have enough colors for all enabled languages
            sColors = context.getResources().getStringArray(R.array.colors);
        }
        mQuestion = q;
        mSurveyListener = surveyListener;
        mError = null;// so far so good.
    }

    /**
     * Inflate the appropriate layout file, and retrieve the references to the common resources.
     * Subclasses' layout files should ALWAYS contain the question_header view.
     * Inflated layout will be attached to the View's root, thus all the elements within it
     * will be accessible by calling findViewById(int)
     *
     * @param layoutRes resource containing the layout for the question.
     */
    protected void setQuestionView(int layoutRes) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(layoutRes, this, true);

        mQuestionText = (TextView) findViewById(R.id.question_tv);
        mTipImage = (ImageButton) findViewById(R.id.tip_ib);

        if (mQuestionText == null || mTipImage == null) {
            throw new RuntimeException(
                    "Subclasses must inflate the common question header before calling this method.");
        }

        mQuestionText.setText(formText(), BufferType.SPANNABLE);

        // if there is a tip for this question, construct an alert dialog box with the data
        final int tips = mQuestion.getHelpTypeCount();
        if (tips > 0) {
            mTipImage.setVisibility(View.VISIBLE);// GONE by default
            mTipImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tips > 1) {
                        displayHelpChoices();
                    } else {
                        if (mQuestion.getHelpByType(ConstantUtil.TIP_HELP_TYPE)
                                .size() > 0) {
                            displayHelp(ConstantUtil.TIP_HELP_TYPE);
                        } else if (mQuestion.getHelpByType(
                                ConstantUtil.VIDEO_HELP_TYPE).size() > 0) {
                            displayHelp(ConstantUtil.VIDEO_HELP_TYPE);
                        } else if (mQuestion.getHelpByType(
                                ConstantUtil.IMAGE_HELP_TYPE).size() > 0) {
                            displayHelp(ConstantUtil.IMAGE_HELP_TYPE);
                        }
                    }
                }
            });
        }

        if (!isReadOnly()) {
            mQuestionText.setLongClickable(true);
            mQuestionText.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    onClearAnswer();
                    return true;
                }
            });
        }

        // if this question has 1 or more dependencies, then it needs to be invisible initially
        if (mQuestion.getDependencies() != null && mQuestion.getDependencies().size() > 0) {
            setVisibility(View.GONE);
        }
    }

    protected void onClearAnswer() {
        ViewUtil.showConfirmDialog(R.string.clearquestion,
                R.string.clearquestiondesc, getContext(), true,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetQuestion(true);
                        checkMandatory();
                    }
                });
    }

    /**
     * forms the question text based on the selected languages
     *
     * @return
     */
    private Spanned formText() {
        boolean isFirst = true;
        StringBuilder text = new StringBuilder();
        if (mQuestion.isMandatory()) {
            text.append("<b>");
        }

        text.append(mQuestion.getOrder()).append(". ");// Prefix the text with the order
        final String[] langs = getLanguages();
        final String language = getDefaultLang();

        for (int i = 0; i < langs.length; i++) {
            if (language.equalsIgnoreCase(langs[i])) {
                if (!isFirst) {
                    text.append(" / ");
                } else {
                    isFirst = false;
                }
                text.append(mQuestion.getText());
            } else {
                AltText txt = mQuestion.getAltText(langs[i]);
                if (txt != null) {
                    if (!isFirst) {
                        text.append(" / ");
                    } else {
                        isFirst = false;
                    }
                    text.append("<font color='").append(sColors[i % sColors.length]).append("'>")
                            .append(txt.getText()).append("</font>");
                }
            }
        }
        if (mQuestion.isMandatory()) {
            text = text.append("*</b>");
        }
        return Html.fromHtml(text.toString());
    }

    public void notifyOptionsChanged() {
        mQuestionText.setText(formText(), BufferType.SPANNABLE);
    }

    /**
     * displays a dialog box with options for each of the help types that have
     * been initialized for this particular question.
     */
    @SuppressWarnings("rawtypes")
    private void displayHelpChoices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.helpheading);
        final CharSequence[] items = new CharSequence[mQuestion
                .getHelpTypeCount()];
        final Resources resources = getResources();
        int itemIndex = 0;
        List tempList = mQuestion.getHelpByType(ConstantUtil.IMAGE_HELP_TYPE);

        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.photohelpoption);
        }
        tempList = mQuestion.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE);
        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.videohelpoption);
        }
        tempList = mQuestion.getHelpByType(ConstantUtil.TIP_HELP_TYPE);
        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.texthelpoption);
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String val = items[id].toString();
                if (resources.getString(R.string.texthelpoption).equals(val)) {
                    displayHelp(ConstantUtil.TIP_HELP_TYPE);
                } else if (resources.getString(R.string.videohelpoption)
                        .equals(val)) {
                    displayHelp(ConstantUtil.VIDEO_HELP_TYPE);
                } else if (resources.getString(R.string.photohelpoption)
                        .equals(val)) {
                    displayHelp(ConstantUtil.IMAGE_HELP_TYPE);
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * displays the selected help type
     *
     * @param type
     */
    private void displayHelp(String type) {
        if (ConstantUtil.VIDEO_HELP_TYPE.equals(type)) {
            notifyQuestionListeners(QuestionInteractionEvent.VIDEO_TIP_VIEW);
        } else if (ConstantUtil.TIP_HELP_TYPE.equals(type)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            StringBuilder textBuilder = new StringBuilder();
            List<QuestionHelp> helpItems = mQuestion.getHelpByType(type);
            boolean isFirst = true;
            String[] langs = getLanguages();
            String language = getDefaultLang();
            if (helpItems != null) {
                for (int i = 0; i < helpItems.size(); i++) {
                    if (i > 0) {
                        textBuilder.append("<br>");
                    }

                    for (int j = 0; j < langs.length; j++) {
                        if (language.equalsIgnoreCase(langs[j])) {
                            textBuilder.append(helpItems.get(i).getText());
                            isFirst = false;
                        }

                        AltText aText = helpItems.get(i).getAltText(langs[j]);
                        if (aText != null) {
                            if (!isFirst) {
                                textBuilder.append(" / ");
                            } else {
                                isFirst = false;
                            }

                            textBuilder.append("<font color='").append(sColors[j]).append("'>")
                                    .append(aText.getText()).append("</font>");
                        }
                    }
                }
            }
            builder.setMessage(Html.fromHtml(textBuilder.toString()));
            builder.setPositiveButton(R.string.okbutton,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.show();
        } else if (ConstantUtil.IMAGE_HELP_TYPE.equals(type)) {
            notifyQuestionListeners(QuestionInteractionEvent.PHOTO_TIP_VIEW);
        } else {
            notifyQuestionListeners(QuestionInteractionEvent.ACTIVITY_TIP_VIEW);
        }
    }

    /**
     * adds a listener to the internal list of clients to be notified on an
     * event
     *
     * @param listener
     */
    public void addQuestionInteractionListener(QuestionInteractionListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<QuestionInteractionListener>();
        }
        if (listener != null && !mListeners.contains(listener) && listener != this) {
            mListeners.add(listener);
        }
    }

    /**
     * notifies each QuestionInteractionListener registered with this question.
     * This is done serially on the calling thread.
     */
    protected void notifyQuestionListeners(String type, Bundle data) {
        if (mListeners != null) {
            QuestionInteractionEvent event = new QuestionInteractionEvent(type, this, data);
            for (int i = 0; i < mListeners.size(); i++) {
                mListeners.get(i).onQuestionInteraction(event);
            }
        }
    }

    protected void notifyQuestionListeners(String type) {
        notifyQuestionListeners(type, null);
    }

    /**
     * method that can be overridden by sub classes if they want to have some
     * sort of visual response to a question interaction.
     */
    public void questionComplete(Bundle data) {
        // do nothing
    }

    /**
     * method that should be overridden by sub classes to clear current value
     */
    public void resetQuestion(boolean fireEvent) {
        boolean suppressListeners = !fireEvent;
        setResponse(null, suppressListeners);
        setError(null);
        if (fireEvent) {
            notifyQuestionListeners(QuestionInteractionEvent.QUESTION_CLEAR_EVENT);
        }

        checkDependencies();
    }

    /**
     * Show/Hide the Question, according to the dependencies
     */
    public void checkDependencies() {
        if (areDependenciesSatisfied()) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    public void onQuestionInteraction(QuestionInteractionEvent event) {
        if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event.getEventType())) {
            // if this question is dependent, see if it has been satisfied
            List<Dependency> dependencies = mQuestion.getDependencies();
            if (dependencies != null) {
                for (int i = 0; i < dependencies.size(); i++) {
                    Dependency d = dependencies.get(i);
                    if (d.getQuestion().equalsIgnoreCase(
                            event.getSource().getQuestion().getId())) {
                        if (handleDependencyParentResponse(d, event.getSource().getResponse())) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * updates the state of this question view based on the value in the
     * dependency parent response. This method returns true if there is a value
     * match and false otherwise.
     *
     * @param dep
     * @param resp
     * @return
     */
    public boolean handleDependencyParentResponse(Dependency dep, QuestionResponse resp) {
        boolean isMatch = false;
        if (dep.getAnswer() != null
                && resp != null
                && dep.isMatch(resp.getValue())
                && resp.getIncludeFlag()) {
            isMatch = true;
        } else if (dep.getAnswer() != null
                && resp != null
                && resp.getIncludeFlag()) {
            if (resp.getValue() != null) {
                StringTokenizer strTok = new StringTokenizer(resp.getValue(),
                        "|");
                while (strTok.hasMoreTokens()) {
                    if (dep.isMatch(strTok.nextToken().trim())) {
                        isMatch = true;
                    }
                }
            }
        }

        boolean setVisible = false;
        // if we're here, then the question on which we depend
        // has been answered. Check the value to see if it's the
        // one we are looking for
        if (isMatch) {
            setVisibility(View.VISIBLE);
            if (mResponse != null) {
                mResponse.setIncludeFlag(true);
            }
            setVisible = true;
        } else {
            if (mResponse != null) {
                mResponse.setIncludeFlag(false);
            }
            setVisibility(View.GONE);
        }

        // now notify our own listeners to make sure we correctly toggle
        // nested dependencies (i.e. if A -> B -> C and C changes, A needs to
        // know too).
        if (mResponse != null) {
            notifyQuestionListeners(QuestionInteractionEvent.QUESTION_ANSWER_EVENT);
        }

        return setVisible;
    }

    public final void captureResponse() {
        captureResponse(false);
    }

    /**
     * this method should be overridden by subclasses so they can record input
     * in a QuestionResponse object
     */
    public abstract void captureResponse(boolean suppressListeners);

    /**
     * this method should be overridden by subclasses so they can manage the UI
     * changes when resetting the value
     *
     * @param resp
     */
    public void rehydrate(QuestionResponse resp) {
        setResponse(resp);
    }

    /**
     * Release any heavy resource associated with this view. This method will
     * likely be overridden by subclasses. This callback should ALWAYS be called
     * when the Activity is about to become invisible (paused, stopped,...) and
     * this View's responses have been successfully cached. Any resource that
     * can cause a memory leak or prevent this View from being GC should be
     * freed/notified
     */
    public void onPause() {
    }

    /**
     * Instantiate any resource that depends on the Activity life-cycle (i.e. internal DB connections)
     * This callback will be invoked *after* the question is instantiated and initialized.
     */
    public void onResume() {
    }

    /**
     * Activity callback propagation. Use this hook to release resources no longer needed.
     */
    public void onDestroy() {
    }

    public QuestionResponse getResponse() {
        return mResponse;
    }

    public void setResponse(QuestionResponse response) {
        setResponse(response, false);
    }

    public void setResponse(QuestionResponse response, boolean suppressListeners) {
        if (response != null) {
            if (this.mResponse == null) {
                this.mResponse = response;
            } else {
                // we need to preserve the ID so we don't get duplicates in the db
                this.mResponse.setType(response.getType());
                this.mResponse.setValue(response.getValue());
                this.mResponse.setFilename(response.getFilename());
            }
        } else {
            this.mResponse = response;
        }
        if (!suppressListeners) {
            notifyQuestionListeners(QuestionInteractionEvent.QUESTION_ANSWER_EVENT);
        }
        setError(null);// Reset any error status
    }

    public Question getQuestion() {
        return mQuestion;
    }

    public void setTextSize(float size) {
        mQuestionText.setTextSize(size);
    }

    /**
     * hides or shows the tips button
     *
     * @param isSuppress
     */
    public void suppressHelp(boolean isSuppress) {
        mTipImage.setVisibility(isSuppress ? View.GONE : View.VISIBLE);
    }

    protected String getDefaultLang() {
        return mSurveyListener.getDefaultLanguage();
    }

    protected String[] getLanguages() {
        return mSurveyListener.getLanguages();
    }

    protected boolean isReadOnly() {
        return mSurveyListener.isReadOnly();
    }

    public void setError(String error) {
        mError = error;
        displayError(mError);
    }

    /**
     * displayError will give visual feedback of non-valid responses.
     * By default, we display the message in the question text, although subclasses may
     * override the method and display it elsewhere (i.e. within an EditText)
     *
     * @param error Error text
     */
    public void displayError(String error) {
        mQuestionText.setError(error);
    }

    public void checkMandatory() {
        if (mError == null && mQuestion.isMandatory() &&
                (mResponse == null || !mResponse.isValid())) {
            // Mandatory questions must have a response
            setError(getResources().getString(R.string.error_question_mandatory));
        }
    }

    /**
     * isValid determines if the QuestionView contains a valid status.
     * An invalid status can be set either explicitly with setError(String),
     * or it can be automatically deducted if the question is mandatory and no response is set.
     *
     * @return true if the status is valid, false otherwise
     */
    public boolean isValid() {
        return mError == null;
    }

    public boolean isDoubleEntry() {
        return mQuestion != null ? mQuestion.isDoubleEntry() : false;// Avoid NPE
    }

    /**
     * Checks if the dependencies for the question passed in are satisfied
     *
     * @return true if no dependency is broken, false otherwise
     */
    public boolean areDependenciesSatisfied() {
        List<Dependency> dependencies = getQuestion().getDependencies();
        if (dependencies != null) {
            Map<String, QuestionResponse> responses = mSurveyListener.getResponses();
            for (Dependency dependency : dependencies) {
                QuestionResponse resp = responses.get(dependency.getQuestion());
                if (resp == null || !resp.hasValue()
                        || !dependency.isMatch(resp.getValue())
                        || !resp.getIncludeFlag()) {
                    return false;
                }
            }
        }
        return true;
    }

}

