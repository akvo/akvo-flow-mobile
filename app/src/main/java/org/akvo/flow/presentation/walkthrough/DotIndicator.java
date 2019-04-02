/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.walkthrough;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.akvo.flow.R;

public class DotIndicator extends LinearLayout implements ViewPager.OnPageChangeListener {

    public static final int INVALID_POSITION = -1;

    private int numberOfDots = 0;

    private int selectedItemPosition = INVALID_POSITION;

    @DrawableRes
    private int indicatorInactive;

    @DrawableRes
    private int indicatorActive;

    public DotIndicator(Context context) {
        this(context, null);
    }

    public DotIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        if (attrs != null) {
            TypedArray typedArray = getContext()
                    .obtainStyledAttributes(attrs, R.styleable.DotIndicator);
            if (typedArray != null) {
                indicatorActive = typedArray.getResourceId(R.styleable.DotIndicator_indicatorActive,
                        R.drawable.indicator_active);
                indicatorInactive = typedArray
                        .getResourceId(R.styleable.DotIndicator_indicatorInactive,
                                R.drawable.indicator_inactive);
                numberOfDots = typedArray
                        .getInteger(R.styleable.DotIndicator_indicatorNumber, 3);
                typedArray.recycle();
            }
        }
        setNumberOfDots(numberOfDots);
        onPageSelected(0);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //EMPTY
    }

    @Override
    public void onPageSelected(int position) {
        int lastSelected = selectedItemPosition;
        this.selectedItemPosition = position;
        if (position > INVALID_POSITION) {
            setDotResource(position, indicatorActive);
        }
        if (lastSelected > INVALID_POSITION) {
            setDotResource(lastSelected, indicatorInactive);
        }
    }

    private void setDotResource(int position, int imageResource) {
        ImageView childAt = (ImageView) getChildAt(position);
        if (childAt != null) {
            childAt.setImageResource(imageResource);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //EMPTY
    }

    public void setNumberOfDots(int numberOfDots) {
        this.numberOfDots = numberOfDots;
        removeAllViews();
        for (int x = 0; x < numberOfDots; x++) {
            ImageView v = createDotView();
            addView(v);
        }
    }

    @NonNull
    private ImageView createDotView() {
        ImageView v = new ImageView(getContext());
        int px = 4 * (getResources().getDisplayMetrics().densityDpi / 160);
        LayoutParams params =
                new LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                        LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(px, 0, px, 0);
        v.setLayoutParams(params);
        v.setImageResource(indicatorInactive);
        return v;
    }
}
