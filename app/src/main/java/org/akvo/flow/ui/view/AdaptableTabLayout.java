/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.tabs.TabLayout;

import org.akvo.flow.util.ViewUtil;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

/**
 * a TabLayout which adapts its tabs sizes to the number of items in the pager adapter.
 * For 1 tab -> use all the screen width for tab width
 * For 2 tabs -> stretch both tabs to occupy both half of screen
 * For 3 tabs or more -> tabs will be scrollable and each tab will have the size of about 1/3 of the
 * screen size
 */
public class AdaptableTabLayout extends TabLayout {

    private static final String REQUESTED_TAB_MIN_WIDTH = "requestedTabMinWidth";
    private static final String REQUESTED_TAB_MAX_WIDTH = "requestedTabMaxWidth";
    private static final int DIVIDER_MINIMUM = 1;
    private static final int DIVIDER_MAXIMUM = 3;
    public static final int UNSET = -1;
    private AdapterChangeListener mAdapterChangeListener;
    private int numberOfTabs = UNSET;

    public AdaptableTabLayout(Context context) {
        super(context);
    }

    public AdaptableTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdaptableTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setupWithViewPager(@Nullable ViewPager viewPager) {
        setUpTabSizesUsingViewPagerContent(viewPager);
        setUpAdapterChangeListener(viewPager);
        super.setupWithViewPager(viewPager);
    }

    private void setUpAdapterChangeListener(ViewPager viewPager) {
        if (mAdapterChangeListener == null) {
            mAdapterChangeListener = new AdapterChangeListener();
        }
        viewPager.addOnAdapterChangeListener(mAdapterChangeListener);
    }

    private void setUpTabSizesUsingViewPagerContent(ViewPager viewPager) {
        if (viewPager != null) {
            final PagerAdapter adapter = viewPager.getAdapter();
            if (adapter != null && adapter.getCount() > 0) {
                setTabWidth(adapter.getCount());
            }
        }
    }

    private void setTabWidth(int numberOfTabs) {
        if (this.numberOfTabs != numberOfTabs) {
            this.numberOfTabs = numberOfTabs;
            int width = ViewUtil.getScreenWidth(getContext());
            int divider = Math.min(Math.max(numberOfTabs, DIVIDER_MINIMUM), DIVIDER_MAXIMUM);
            int tabMinWidth = width / divider;
            setMinimumTabWidth(tabMinWidth);
            setMaximumTabWidth(tabMinWidth);
        }
    }

    private void setMinimumTabWidth(int tabMinWidth) {
        initReflectedValue(tabMinWidth, REQUESTED_TAB_MIN_WIDTH);
    }

    private void setMaximumTabWidth(int tabMaxWidth) {
        initReflectedValue(tabMaxWidth, REQUESTED_TAB_MAX_WIDTH);
    }

    private void initReflectedValue(int value, String reflectedFieldName) {
        Field field;
        try {
            field = TabLayout.class.getDeclaredField(reflectedFieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (NoSuchFieldException e) {
            Timber.e(e, "Field not found : %s", reflectedFieldName);
        } catch (IllegalAccessException e) {
            Timber.e(e, "Illegal access : %s", reflectedFieldName);
        }
    }

    private class AdapterChangeListener implements ViewPager.OnAdapterChangeListener {

        @Override
        public void onAdapterChanged(@NonNull ViewPager viewPager,
                @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {
            int count = newAdapter == null ? 0 : newAdapter.getCount();
            setTabWidth(count);
        }
    }
}
