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

package org.akvo.flow.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.ui.fragment.QuestionGroupFragment;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SurveyActivity extends ActionBarActivity implements TabListener,
        QuestionInteractionListener, QuestionGroupFragment.OnFragmentInteractionListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    private static final int PHOTO_ACTIVITY_REQUEST = 1;
    private static final int VIDEO_ACTIVITY_REQUEST = 2;
    private static final int SCAN_ACTIVITY_REQUEST  = 3;

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    /**
     * When a request is done to perform photo, video, barcode scan, etc
     * we store the question id, so we can notify later the status of such
     * operation.
     * TODO: Design how to notify the result back. Broadcast notification?
     */
    private String mRequestQuestionId;

    private ViewPager mPager;
    private TabsAdapter mAdapter;

    private long mSurveyInstanceId;// TODO: Load/Create survey instance
    private Survey mSurvey;
    private SurveyDbAdapter mDatabase;

    private String[] mLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        // Load survey
        final String surveyId = getIntent().getStringExtra(ConstantUtil.SURVEY_ID_KEY);
        loadSurvey(surveyId);
        loadLanguages();

        if (mSurvey == null) {
            Log.e(TAG, "mSurvey is null. Finishing the Activity...");
            finish();
        }

        // Set the survey name as Activity title
        setTitle(mSurvey.getName());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setSelectedNavigationItem(position);
            }
        });
        setupActionBar();
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            // load from file
            in = FileUtil.getFileInputStream(surveyMeta.getFileName(),
                    ConstantUtil.DATA_DIR, false, this);
            mSurvey = SurveyDao.loadSurvey(surveyMeta, in);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        for (QuestionGroup group : mSurvey.getQuestionGroups()) {
            actionBar.addTab(actionBar.newTab()
                    .setText(group.getHeading())
                    .setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.records_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public QuestionGroup getQuestionGroup(int position) {
        return mSurvey.getQuestionGroups().get(position);
    }

    @Override
    public String getSurveyId() {
        return mSurvey.getId();
    }

    @Override
    public long getSurveyInstanceId () {
        return mSurveyInstanceId;
    }

    @Override
    public String getDefaultLang() {
        String lang = mSurvey.getLanguage();
        if (TextUtils.isEmpty(lang)) {
            lang = ConstantUtil.ENGLISH_CODE;
        }
        return lang;
    }

    @Override
    public String[] getLanguages() {
        return mLanguages;
    }

    private void loadLanguages() {
        String langsSelection = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY);
        LangsPreferenceData langsPrefData = LangsPreferenceUtil.createLangPrefData(this,
                langsSelection, langsPresentIndexes);
        mLanguages = LangsPreferenceUtil.getSelectedLangCodes(this,
                langsPrefData.getLangsSelectedMasterIndexArray(),
                langsPrefData.getLangsSelectedBooleanArray(),
                R.array.alllanguagecodes);
    }

    /**
     * event handler that can be used to handle events fired by individual
     * questions at the Activity level. Because we can't launch the photo
     * activity from a view (we need to launch it from the activity), the photo
     * question view fires a QuestionInteractionEvent (to which this activity
     * listens). When we get the event, we can then spawn the camera activity.
     * Currently, this method supports handing TAKE_PHOTO_EVENT and
     * VIDEO_TIP_EVENT types
     */
    public void onQuestionInteraction(QuestionInteractionEvent event) {
        if (QuestionInteractionEvent.TAKE_PHOTO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_PHOTO_NAME_PREFIX + IMAGE_SUFFIX)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, PHOTO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, VIDEO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.SCAN_BARCODE_EVENT.equals(event.getEventType())) {
            Intent intent = new Intent(ConstantUtil.BARCODE_SCAN_INTENT);
            try {
                startActivityForResult(intent, SCAN_ACTIVITY_REQUEST);
                if (event.getSource() != null) {
                    mRequestQuestionId = event.getSource().getQuestion().getId();
                } else {
                    Log.e(TAG, "Question source was null in the event");
                }
            } catch (ActivityNotFoundException ex) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.barcodeerror);
                builder.setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            }
        } else if (QuestionInteractionEvent.QUESTION_CLEAR_EVENT.equals(event.getEventType())) {
            mDatabase.deleteResponse(mSurveyInstanceId, event.getSource().getQuestion().getId());
        }
    }

    class TabsAdapter extends FragmentPagerAdapter {
        
        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mSurvey.getQuestionGroups().size();
        }
        
        private Fragment getFragment(int pos){
            // Hell of a hack. This should be changed for a more reliable method
            String tag = "android:switcher:" + R.id.pager + ":" + pos;
            return getSupportFragmentManager().findFragmentByTag(tag);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = QuestionGroupFragment.newInstance(position);
            return fragment;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return mSurvey.getQuestionGroups().get(position).getHeading();
        }
        
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }
    
}
