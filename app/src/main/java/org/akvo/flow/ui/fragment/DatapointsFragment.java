/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.ConstantUtil;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class DatapointsFragment extends Fragment {

    private static final String TAG = DatapointsFragment.class.getSimpleName();

    private static final int POSITION_LIST = 0;
    private static final int POSITION_MAP = 1;
    private static final String STATS_DIALOG_FRAGMENT_TAG = "stats";

    /**
     * BroadcastReceiver to notify of records synchronisation. This should be
     * fired from SurveyedLocalesSyncService.
     */
    private final BroadcastReceiver mSurveyedLocalesSyncReceiver = new DataPointSyncBroadcastReceiver(
            this);

    private SurveyDbAdapter mDatabase;
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
        args.putSerializable(SurveyActivity.EXTRA_SURVEY_GROUP, surveyGroup);
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
                .getSerializable(SurveyActivity.EXTRA_SURVEY_GROUP);
        tabNames = getResources().getStringArray(R.array.records_activity_tabs);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }
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
        mDatabase.deleteEmptyRecords();

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mSurveyedLocalesSyncReceiver,
                        new IntentFilter(ConstantUtil.ACTION_LOCALE_SYNC));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mSurveyedLocalesSyncReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.datapoints_fragment, container, false);
        mPager = (ViewPager) v.findViewById(R.id.pager);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);

        // Init tabs
        mTabsAdapter = new TabsAdapter(getFragmentManager(), tabNames, mSurveyGroup);
        mPager.setAdapter(mTabsAdapter);
        tabs.setViewPager(mPager);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSurveyGroup != null) {
            inflater.inflate(R.menu.datapoints_fragment, menu);
            SubMenu subMenu = menu.findItem(R.id.more_submenu).getSubMenu();
            if (!mSurveyGroup.isMonitored()) {
                subMenu.removeItem(R.id.sync_records);
            }

            // "Order By" is only available for the ListFragment, not the MapFragment.
            // The navigation components maintain 2 different indexes: Tab index and Pager index.
            // The system seems to always update the tab index first, prior to the onCreateOptionsMenu
            // call (either selecting the Tab or swiping the Pager). For this reason, we need to check
            // the Tab index, not the Pager one, which turns out to be buggy in some Android versions.
            // TODO: If this approach is still unreliable, we'll need to invalidate the menu twice.
            if (mPager != null && mPager.getCurrentItem() == POSITION_MAP) {
                //TODO: maybe instead of removing we should use custom menu for each fragment
                subMenu.removeItem(R.id.order_by);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_datapoint:
                if (listener != null) {
                    String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
                    listener.onRecordSelected(newLocaleId);
                }
                return true;
            case R.id.search:
                if (listener != null) {
                    return listener.onSearchTap();
                }
            case R.id.sync_records:
                if (listener != null && mSurveyGroup != null) {
                    listener.onSyncRecordsTap(mSurveyGroup.getId());
                }
                return true;
            case R.id.stats:
                StatsDialogFragment dialogFragment = StatsDialogFragment
                        .newInstance(mSurveyGroup.getId());
                dialogFragment.show(getFragmentManager(), STATS_DIALOG_FRAGMENT_TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static class TabsAdapter extends FragmentPagerAdapter {

        private final String[] tabs;
        private SurveyGroup surveyGroup;
        private final Map<Integer, Fragment> fragmentsRef = new WeakHashMap<>(2);

        public TabsAdapter(FragmentManager fm, String[] tabs, SurveyGroup surveyGroup) {
            super(fm);
            this.tabs = tabs;
            this.surveyGroup = surveyGroup;
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        public void refreshFragments(SurveyGroup newSurveyGroup) {
            this.surveyGroup = newSurveyGroup;
            SurveyedLocaleListFragment listFragment = (SurveyedLocaleListFragment) fragmentsRef
                    .get(POSITION_LIST);
            MapFragment mapFragment = (MapFragment) fragmentsRef.get(POSITION_MAP);

            if (listFragment != null) {
                listFragment.refresh(surveyGroup);
            }
            if (mapFragment != null) {
                mapFragment.refresh(surveyGroup);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (position == POSITION_LIST) {
                SurveyedLocaleListFragment surveyedLocaleListFragment = (SurveyedLocaleListFragment) super
                        .instantiateItem(container, position);
                fragmentsRef.put(POSITION_LIST, surveyedLocaleListFragment);
                return surveyedLocaleListFragment;
            } else {
                MapFragment mapFragment = (MapFragment) super.instantiateItem(container, position);
                fragmentsRef.put(POSITION_MAP, mapFragment);
                return mapFragment;
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position == POSITION_LIST) {
                return SurveyedLocaleListFragment.newInstance(surveyGroup);
            }
            // Map mode
            return MapFragment.newInstance(surveyGroup, null);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs[position];
        }

    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        refreshView();
    }

    private void refreshView() {
        if (mTabsAdapter != null) {
            mTabsAdapter.refreshFragments(mSurveyGroup);
        }
        if (listener != null) {
            listener.refreshMenu();
        }
    }

    private static class DataPointSyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<DatapointsFragment> fragmentWeakReference;

        private DataPointSyncBroadcastReceiver(DatapointsFragment fragment) {
            this.fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "New Records have been synchronised. Refreshing fragments...");
            DatapointsFragment datapointsFragment = fragmentWeakReference.get();
            if (datapointsFragment != null) {
                datapointsFragment.refreshView();
            }
        }
    }

    public interface DatapointFragmentListener {

        void refreshMenu();

        void onRecordSelected(String recordId);

        boolean onSearchTap();

        void onSyncRecordsTap(long surveyGroupId);
    }
}
