/*
 * Copyright (C) 2010-2017,2019 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.datapoints.list.DataPointsListFragment;
import org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment;
import org.akvo.flow.presentation.survey.FABListener;
import org.akvo.flow.tracking.TrackingListener;
import org.akvo.flow.util.ConstantUtil;

import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class DatapointsFragment extends Fragment {

    private static final int POSITION_LIST = 0;
    private static final int POSITION_MAP = 1;
    private static final String STATS_DIALOG_FRAGMENT_TAG = "stats";

    @Inject
    SurveyDbDataSource mDatabase;

    private TabsAdapter mTabsAdapter;
    private SurveyGroup mSurveyGroup;

    private String[] tabNames;

    private ViewPager mPager;

    private TrackingListener trackingListener;
    private FABListener fabListener;

    public DatapointsFragment() {
    }

    public static DatapointsFragment newInstance(SurveyGroup surveyGroup) {
        DatapointsFragment fragment = new DatapointsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (! (context instanceof TrackingListener)) {
            throw new IllegalArgumentException("Activity must implement TrackingListener");
        } else {
            trackingListener = (TrackingListener) context;
        }
        if (! (context instanceof FABListener)) {
            throw new IllegalArgumentException("Activity must implement FABListener");
        } else {
            fabListener = (FABListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        trackingListener = null;
        fabListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector();
        mSurveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.SURVEY_EXTRA);
        tabNames = getResources().getStringArray(R.array.records_activity_tabs);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.datapoints_fragment, container, false);
        mPager = v.findViewById(R.id.pager);
        TabLayout tabs = v.findViewById(R.id.tabs);

        mTabsAdapter = new TabsAdapter(getChildFragmentManager(), tabNames, mSurveyGroup);
        mPager.setAdapter(mTabsAdapter);
        tabs.setupWithViewPager(mPager);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {
                //EMPTY
            }

            @Override
            public void onPageSelected(int position) {
                if (position == POSITION_MAP && mSurveyGroup != null) {
                    DataPointsMapFragment mapFragment = mTabsAdapter.getMapFragment();
                    if (mapFragment != null) {
                        mapFragment.showFab();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_SETTLING:
                    case ViewPager.SCROLL_STATE_IDLE:
                        showFabs();
                        break;
                    default:
                        hideFabs();
                        break;
                }
            }
        });

        return v;
    }

    private void hideFabs() {
        if (fabListener != null) {
            fabListener.hideFab();
        }
        if (mSurveyGroup != null) {
            DataPointsMapFragment mapFragment = mTabsAdapter.getMapFragment();
            if (mapFragment != null) {
                mapFragment.hideFab();
            }
        }
    }

    private void showFabs() {
        if (fabListener != null) {
            fabListener.showFab();
        }

        if (mPager.getCurrentItem() == POSITION_MAP && mSurveyGroup != null) {
            DataPointsMapFragment mapFragment = mTabsAdapter.getMapFragment();
            if (mapFragment != null) {
                mapFragment.showFab();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.stats) {
            StatsDialogFragment dialogFragment = StatsDialogFragment
                    .newInstance(mSurveyGroup.getId());
            dialogFragment.show(getFragmentManager(), STATS_DIALOG_FRAGMENT_TAG);
            int selectedTab = mPager.getCurrentItem();

            if (trackingListener != null) {
                trackingListener.logStatsEvent(selectedTab);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshMap() {
        mTabsAdapter.refreshMap();
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
            DataPointsMapFragment mapFragment = getMapFragment();

            if (listFragment != null) {
                listFragment.onNewSurveySelected(surveyGroup);
            }
            if (mapFragment != null) {
                mapFragment.onNewSurveySelected(surveyGroup);
            }
        }

        DataPointsMapFragment getMapFragment() {
            return (DataPointsMapFragment) fragmentsRef.get(POSITION_MAP);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
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

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == POSITION_LIST) {
                return DataPointsListFragment.newInstance(surveyGroup);
            }
            return DataPointsMapFragment.newInstance(surveyGroup);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs[position];
        }

        public void refreshMap() {
            getMapFragment().refreshView();
        }
    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        getArguments().putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup);
        refreshView();
    }

    private void refreshView() {
        if (mTabsAdapter != null) {
            mTabsAdapter.refreshFragments(mSurveyGroup);
        }
    }
}
