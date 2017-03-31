/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.datapoints.list.DataPointsListFragment;
import org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

public class DatapointsFragment extends Fragment {

    private static final int POSITION_LIST = 0;
    private static final int POSITION_MAP = 1;
    private static final String STATS_DIALOG_FRAGMENT_TAG = "stats";

    @Inject
    SurveyDbDataSource mDatabase;

    private TabsAdapter mTabsAdapter;
    private ViewPager mPager;
    private SurveyGroup mSurveyGroup;

    @Nullable
    private DatapointFragmentListener listener;

    private String[] tabNames;

    public DatapointsFragment() {
    }

    public static DatapointsFragment newInstance(SurveyGroup surveyGroup) {
        DatapointsFragment fragment = new DatapointsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.EXTRA_SURVEY_GROUP, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof DatapointFragmentListener)) {
            throw new IllegalArgumentException("Activity must implement DatapointFragmentListener");
        }
        this.listener = (DatapointFragmentListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.EXTRA_SURVEY_GROUP);
        tabNames = getResources().getStringArray(R.array.records_activity_tabs);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();
        mDatabase.open();
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Delete empty Records, if any
        // TODO: For a more efficient cleanup, attempt to wipe ONLY the latest Record,
        // TODO: providing the id to RecordActivity, and reading it back on onActivityResult(...)
        // TODO: this is very strange, verify what it does and move it to some service
        mDatabase.deleteEmptyRecords();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.datapoints_fragment, container, false);
        mPager = (ViewPager) v.findViewById(R.id.pager);
        TabLayout tabs = (TabLayout) v.findViewById(R.id.tabs);

        // Init tabs
        mTabsAdapter = new TabsAdapter(getChildFragmentManager(), tabNames, mSurveyGroup);
        mPager.setAdapter(mTabsAdapter);
        tabs.setupWithViewPager(mPager);

        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_datapoint:
                if (listener != null) {
                    String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId(),
                            PlatformUtil.recordUuid());
                    listener.onRecordSelected(newLocaleId);
                }
                return true;
            case R.id.search:
                if (listener != null) {
                    return listener.onSearchTap();
                }
            case R.id.stats:
                StatsDialogFragment dialogFragment = StatsDialogFragment
                        .newInstance(mSurveyGroup.getId());
                dialogFragment.show(getFragmentManager(), STATS_DIALOG_FRAGMENT_TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static class TabsAdapter extends FragmentPagerAdapter {

        private final String[] tabs;
        private SurveyGroup surveyGroup;
        private final Map<Integer, Fragment> fragmentsRef = new WeakHashMap<>(2);

        TabsAdapter(FragmentManager fm, String[] tabs, SurveyGroup surveyGroup) {
            super(fm);
            this.tabs = tabs;
            this.surveyGroup = surveyGroup;
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        void refreshFragments(SurveyGroup newSurveyGroup) {
            this.surveyGroup = newSurveyGroup;
            DataPointsListFragment listFragment = (DataPointsListFragment) fragmentsRef
                    .get(POSITION_LIST);
            DataPointsMapFragment mapFragment = (DataPointsMapFragment) fragmentsRef
                    .get(POSITION_MAP);

            if (listFragment != null) {
                listFragment.refresh(surveyGroup);
            }
            if (mapFragment != null) {
                mapFragment.refreshData(surveyGroup);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (position == POSITION_LIST) {
                DataPointsListFragment dataPointsListFragment = (DataPointsListFragment) super
                        .instantiateItem(container, position);
                fragmentsRef.put(POSITION_LIST, dataPointsListFragment);
                return dataPointsListFragment;
            } else {
                DataPointsMapFragment mapFragment = (DataPointsMapFragment) super
                        .instantiateItem(container, position);
                fragmentsRef.put(POSITION_MAP, mapFragment);
                return mapFragment;
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position == POSITION_LIST) {
                return DataPointsListFragment.newInstance(surveyGroup);
            }
            // Map mode
            return DataPointsMapFragment.newInstance(surveyGroup);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs[position];
        }

    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        if (mTabsAdapter != null) {
            mTabsAdapter.refreshFragments(mSurveyGroup);
        }
    }

    public interface DatapointFragmentListener {

        void onRecordSelected(String recordId);

        boolean onSearchTap();
    }
}
