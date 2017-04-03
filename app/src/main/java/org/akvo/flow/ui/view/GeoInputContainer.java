/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.akvo.flow.R;

import java.text.DecimalFormat;

public class GeoInputContainer extends LinearLayout {

    private static final int ALPHA_ANIMATION_DURATION = 50;
    private static final float ALPHA_OPAQUE = 1f;
    private static final float ALPHA_TRANSPARENT = 0.1f;

    private final DecimalFormat accuracyFormat = new DecimalFormat("#");
    private final DecimalFormat altitudeFormat = new DecimalFormat("#.#");

    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText elevationInput;
    private TextView statusIndicator;

    public GeoInputContainer(Context context) {
        this(context, null);
    }

    public GeoInputContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.geo_manual_info_layout, null);
        addView(view);
        latitudeInput = (EditText) findViewById(R.id.lat_et);
        longitudeInput = (EditText) findViewById(R.id.lon_et);
        elevationInput = (EditText) findViewById(R.id.height_et);
        statusIndicator = (TextView) findViewById(R.id.acc_tv);
    }

    void setInputsFocusChangeListeners(GeoQuestionView geoQuestionView) {
        latitudeInput.setOnFocusChangeListener(geoQuestionView);
        longitudeInput.setOnFocusChangeListener(geoQuestionView);
        elevationInput.setOnFocusChangeListener(geoQuestionView);
    }

    void disableInputsFocusability() {
        latitudeInput.setFocusable(false);
        longitudeInput.setFocusable(false);
        elevationInput.setFocusable(false);
    }

    /**
     * setAlpha is only available API >= 11
     */
    private void setAlpha(float originalAlpha, float targetAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final AlphaAnimation animation = new AlphaAnimation(originalAlpha, targetAlpha);
            animation.setDuration(ALPHA_ANIMATION_DURATION);
            animation.setFillAfter(true);
            startAnimation(animation);
        } else {
            setAlpha(targetAlpha);
        }
    }

    private void resetChildViewsToDefaultValues() {
        statusIndicator.setText(R.string.geo_location_accuracy_default);
        latitudeInput.setText("");
        longitudeInput.setText("");
        elevationInput.setText("");
    }

    void displayCoordinates(@NonNull String latitude, @NonNull String longitude, @Nullable Double altitude,
            float accuracy) {
        statusIndicator.setText(getContext()
                .getString(R.string.geo_location_accuracy, accuracyFormat.format(accuracy)));
        displayCoordinates(latitude, longitude, altitude);
    }

    void displayCoordinates(@NonNull String latitude, @NonNull String longitude, @Nullable Double altitude) {
        latitudeInput.setText(latitude);
        longitudeInput.setText(longitude);
        if (altitude != null) {
            elevationInput.setText(altitudeFormat.format(altitude));
        } else {
            elevationInput.setText("");
        }
    }

    void showCoordinatesAccurate() {
        statusIndicator.setTextColor(Color.GREEN);
    }

    void showLocationListenerStopped() {
        setAlpha(ALPHA_TRANSPARENT, ALPHA_OPAQUE);
    }

    void showLocationListenerStarted() {
        setAlpha(ALPHA_OPAQUE, ALPHA_TRANSPARENT);
        resetChildViewsToDefaultValues();
        showCoordinatesInaccurate();
    }

    private void showCoordinatesInaccurate() {
        statusIndicator.setTextColor(Color.RED);
    }

    String getLatitudeText() {
        return latitudeInput.getText().toString();
    }

    String getLongitudeText() {
        return longitudeInput.getText().toString();
    }

    String getElevationText() {
        return elevationInput.getText().toString();
    }
}
