package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.domain.Dependency;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionGroupTab;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.SubmitTab;

import java.util.ArrayList;
import java.util.List;

public class SurveyTabAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener,
        ActionBar.TabListener {
    private static final String TAG = SurveyTabAdapter.class.getSimpleName();

    private Context mContext;
    private SurveyListener mSurveyListener;
    private QuestionInteractionListener mQuestionListener;

    private ActionBar mActionBar;
    private ViewPager mPager;
    private List<QuestionGroup> mQuestionGroups;
    private List<QuestionGroupTab> mQuestionGroupTabs;
    private SubmitTab mSubmitTab;

    public SurveyTabAdapter(Context context, ActionBar actionBar, ViewPager pager,
            SurveyListener surveyListener, QuestionInteractionListener questionListener) {
        mContext = context;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mActionBar = actionBar;
        mPager = pager;
        init();
    }

    private void init() {
        mQuestionGroups = mSurveyListener.getQuestionGroups();
        mQuestionGroupTabs = new ArrayList<QuestionGroupTab>();

        for (QuestionGroup group : mQuestionGroups) {
            QuestionGroupTab questionGroupTab = new QuestionGroupTab(mContext, group,
                    mSurveyListener, mQuestionListener);
            mQuestionGroupTabs.add(questionGroupTab);
        }

        if (!mSurveyListener.isReadOnly()) {
            mSubmitTab = new SubmitTab(mContext, mSurveyListener);
        }

        // Setup the tabs in the action bar
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        for (QuestionGroup group : mQuestionGroups) {
            mActionBar.addTab(mActionBar.newTab()
                    .setText(group.getHeading())
                    .setTabListener(this));
        }

        if (mSubmitTab != null) {
            mActionBar.addTab(mActionBar.newTab()
                    .setText("Submit")// TODO: Externalize string
                    .setTabListener(this));
        }

        mPager.setOnPageChangeListener(this);
    }

    public void notifyOptionsChanged() {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.notifyOptionsChanged();// Spread the word
        }
    }

    /**
     * Check if the tab is loaded, and do so if it has not been loaded yet.
     * @param tab
     */
    private void loadTab(int position) {
        QuestionGroupTab tab = mQuestionGroupTabs.get(position);
        if (!tab.isLoaded()) {
            Log.d(TAG, "Loading Tab #" + position);
            tab.load();
            setupDependencies();// Dependencies might occur across tabs
            tab.loadState();
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

    public void onPause() {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onPause();
        }
    }

    public void onQuestionComplete(String questionId, Bundle data) {
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            questionGroupTab.onQuestionComplete(questionId, data);
        }
    }

    private QuestionView getQuestionView(String questionId) {
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
        for (QuestionGroup group : mQuestionGroups) {
            for (Question question : group.getQuestions()) {
                setupDependencies(question);
            }
        }
    }

    private void setupDependencies(Question question) {
        final List<Dependency> dependencies = question.getDependencies();

        if (dependencies == null) {
            return;// No dependencies for this question
        }

        for (Dependency dependency : dependencies) {
            QuestionView parentQ = getQuestionView(dependency.getQuestion());
            QuestionView depQ = getQuestionView(question.getId());
            if (depQ != null && parentQ != null && depQ != parentQ) {
                parentQ.addQuestionInteractionListener(depQ);
            }
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
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
            loadTab(position);// Check all the tabs have been populated by now
            mSubmitTab.refresh(checkInvalidQuestions());
        }

        // Select the corresponding tab
        mActionBar.setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * checks if all the mandatory questions (on all tabs) have responses
     *
     * @return
     */
    private List<Question> checkInvalidQuestions() {
        List<Question> invalidQuestions = new ArrayList<Question>();
        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
            invalidQuestions.addAll(questionGroupTab.checkInvalidQuestions());
        }

        return invalidQuestions;
    }

}
