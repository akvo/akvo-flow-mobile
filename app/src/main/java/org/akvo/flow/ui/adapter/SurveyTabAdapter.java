/*
 *  Copyright (C) 2014-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionGroupTab;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.SubmitTab;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

public class SurveyTabAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    private final ViewPager mPager;
    private List<QuestionGroup> mQuestionGroups;
    private List<QuestionGroupTab> mQuestionGroupTabs;
    private SubmitTab mSubmitTab;
    private OnTabLoadedListener mOnTabLoadedListener;

    public interface OnTabLoadedListener {
        void onTabLoaded();
    }

    public void setOnTabLoadedListener(OnTabLoadedListener listener) {
        mOnTabLoadedListener = listener;
    }

    public SurveyTabAdapter(Context context, ViewPager pager, SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        mPager = pager;
        init(context, surveyListener, questionListener);
    }

    private void init(Context context, SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        mQuestionGroups = surveyListener.getQuestionGroups();
        mQuestionGroupTabs = new ArrayList<>();

        for (QuestionGroup group : mQuestionGroups) {
            QuestionGroupTab questionGroupTab =
                    new QuestionGroupTab(context, group, surveyListener, questionListener);
            mQuestionGroupTabs.add(questionGroupTab);
        }

        if (!surveyListener.isReadOnly()) {
            mSubmitTab = new SubmitTab(context);
        }

        mPager.addOnPageChangeListener(this);
    }

    public void notifyOptionsChanged() {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.notifyOptionsChanged();// Spread the word
        }
    }

    /**
     * Check if the tab is loaded, and do so if it has not been loaded yet.
     */
    private void loadTab(int position) {
        QuestionGroupTab tab = mQuestionGroupTabs.get(position);
        if (!tab.isLoaded()) {
            Timber.d("Loading Tab #%d", position);
            tab.load();
            tab.loadState();
            setupDependencies();// Dependencies might occur across tabs

            if (mOnTabLoadedListener != null) {
                mOnTabLoadedListener.onTabLoaded();
            }
        }
    }

    public void reset() {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            if (questionGroupTab.isLoaded()) {
                // Only care about the loaded tabs
                questionGroupTab.loadState();
            }
        }
    }

    /**
     * Attempt to display a particular question, based on the given ID.
     * If the question is found, the containing tab will be scrolled to the question's position.
     * Upon success the tab position will be returned, -1 otherwise
     */
    public int displayQuestion(String questionId) {
        for (int i = 0; i < mQuestionGroupTabs.size(); i++) {
            QuestionGroupTab questionGroupTab = mQuestionGroupTabs.get(i);
            if (questionGroupTab.displayQuestion(questionId)) {
                return i;
            }
        }
        return -1;
    }

    public void onPause() {
        // Propagate onPause callback
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onPause();
        }
    }

    public void onResume() {
        // Propagate onResume callback
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onResume();
        }
    }

    public void onDestroy() {
        // Propagate onDestroy callback
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onDestroy();
        }
    }

    public void onQuestionResultReceived(String questionId, Bundle data) {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onQuestionResultReceived(questionId, data);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String questionId, String[] permissions,
            int[] grantResults) {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onRequestPermissionsResult(requestCode, questionId, permissions, grantResults);
        }
    }

    public QuestionView getQuestionView(String questionId) {
        QuestionView questionView = null;
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionView = questionGroupTab.getQuestionView(questionId);
            if (questionView != null) {
                break;
            }
        }
        return questionView;
    }

    /**
     * Sets up question dependencies across question groups and registers
     * questionInteractionListeners on the dependent views. This should be
     * called each time a new tab is hydrated. It will iterate over all
     * questions in the survey and install dependencies and the
     * questionInteractionListeners. After installation, it will check to see if
     * the parent question contains a response. If so, it will fire a
     * questionInteractionEvent to ensure dependent questions are put into the
     * correct state
     */
    private void setupDependencies() {
        for (QuestionGroupTab tab : mQuestionGroupTabs) {
            tab.setupDependencies();
        }
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view;
        if (position < mQuestionGroupTabs.size()) {
            view = mQuestionGroupTabs.get(position);// Already instantiated
            loadTab(position);// Load tab state, if necessary
        } else {
            view = mSubmitTab;
        }
        container.addView(view, 0);
        return view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position < mQuestionGroups.size()) {
            return mQuestionGroups.get(position).getHeading();
        } else {
            return mPager.getContext().getString(R.string.submitbutton);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        if (mSubmitTab != null) {
            return mQuestionGroups.size() + 1;// Do not forget the submit tab
        }
        return mQuestionGroups.size();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position == mQuestionGroupTabs.size() && mSubmitTab != null) {
            // Check all the tabs have been populated by now
            int i = 0;
            while (i < position) {
                loadTab(i++);
            }
            mSubmitTab.refresh(checkInvalidQuestions());
        } else {
            QuestionGroupTab questionGroupTab = mQuestionGroupTabs.get(position);
            if (questionGroupTab != null) {
                questionGroupTab.loadState();
                questionGroupTab.setupDependencies();
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // EMPTY
    }

    /**
     * Checks if all the mandatory questions (on all tabs) have responses
     */
    private List<Question> checkInvalidQuestions() {
        List<Question> invalidQuestions = new ArrayList<>();
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            invalidQuestions.addAll(questionGroupTab.checkInvalidQuestions());
        }

        return invalidQuestions;
    }

}
