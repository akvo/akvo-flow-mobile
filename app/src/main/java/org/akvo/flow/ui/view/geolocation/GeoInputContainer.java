/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.geolocation;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import org.akvo.flow.R;
import org.akvo.flow.ui.view.ErrorMessageFormatter;
import org.akvo.flow.ui.view.ResponseInputWatcher;
import org.akvo.flow.util.LocationValidator;

import java.text.DecimalFormat;

public class GeoInputContainer extends CoordinatorLayout {

    private static final float ALPHA_OPAQUE = 1f;
    private static final float ALPHA_TRANSPARENT = 0.1f;

    private final DecimalFormat accuracyFormat = new DecimalFormat("#");
    private final LocationValidator locationValidator = new LocationValidator();
    private final ErrorMessageFormatter errorMessageFormatter = new ErrorMessageFormatter();

    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText elevationInput;
    private TextView statusIndicator;
    private TextView accuracyWarning;
    private boolean disableWatchers;

    public GeoInputContainer(Context context) {
        this(context, null);
    }

    public GeoInputContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.geo_manual_info_layout, this);
        latitudeInput = (EditText) findViewById(R.id.lat_et);
        longitudeInput = (EditText) findViewById(R.id.lon_et);
        elevationInput = (EditText) findViewById(R.id.height_et);
        statusIndicator = (TextView) findViewById(R.id.acc_tv);
        accuracyWarning = (TextView) findViewById(R.id.accuracy_warning_tv);
        setTextWatchers();
    }

    private void setTextWatchers() {
        latitudeInput.addTextChangedListener(new GeoInputTextWatcher(
                () -> {
                    String latitude = latitudeInput.getText().toString();
                    boolean skipCheckIfEmpty = TextUtils.isEmpty(latitude);
                    if (skipCheckIfEmpty) {
                        return;
                    }
                    if (!locationValidator.isValidLatitude(latitude)) {
                        setTextInputError(latitudeInput, R.string.invalid_latitude);
                    }
                }));

        longitudeInput.addTextChangedListener(new GeoInputTextWatcher(
                () -> {
                    String longitude = longitudeInput.getText().toString();
                    boolean skipCheckIfEmpty = TextUtils.isEmpty(longitude);
                    if (skipCheckIfEmpty) {
                        return;
                    }
                    if (!locationValidator.isValidLongitude(longitude)) {
                        setTextInputError(longitudeInput, R.string.invalid_longitude);
                    }
                }));
        elevationInput.addTextChangedListener(new GeoInputTextWatcher(
                () -> {
                    String elevation = elevationInput.getText().toString();
                    boolean skipCheckIfEmpty = TextUtils.isEmpty(elevation);
                    if (skipCheckIfEmpty) {
                        return;
                    }
                    if (!locationValidator.isValidElevation(elevation)) {
                        setTextInputError(elevationInput, R.string.invalid_elevation);
                    }
                }));
    }

    private void setTextInputError(EditText editText, @StringRes int resId) {
        if (editText != null) {
            SpannableStringBuilder errorSpannable = errorMessageFormatter
                    .getErrorSpannable(getContext().getString(resId));
            editText.setError(errorSpannable);
        }
    }

    void setTextWatchers(GeoQuestionView geoQuestionView) {
        ResponseInputWatcher responseInputWatcher = new ResponseInputWatcher(geoQuestionView) {
            @Override
            public void afterTextChanged(Editable s) {
                if (disableWatchers) {
                    return;
                }
                super.afterTextChanged(s);
            }
        };
        latitudeInput.addTextChangedListener(responseInputWatcher);
        longitudeInput.addTextChangedListener(responseInputWatcher);
        elevationInput.addTextChangedListener(responseInputWatcher);
    }

    void disableManualInputs() {
        latitudeInput.setFocusable(false);
        latitudeInput.setEnabled(false);
        longitudeInput.setFocusable(false);
        longitudeInput.setEnabled(false);
        elevationInput.setFocusable(false);
        elevationInput.setEnabled(false);
    }

    private void resetChildViewsToDefaultValues() {
        disableWatchers = true;
        statusIndicator.setText(R.string.geo_location_accuracy_default);
        latitudeInput.setText("");
        longitudeInput.setText("");
        elevationInput.setText("");
        disableWatchers = false;
    }

    void displayCoordinates(@NonNull String latitude, @NonNull String longitude,
                            @Nullable String altitude, float accuracy) {
        statusIndicator.setText(getContext()
                .getString(R.string.geo_location_accuracy, accuracyFormat.format(accuracy)));
        displayCoordinates(latitude, longitude, altitude);
    }

    void displayCoordinates(@NonNull String latitude, @NonNull String longitude,
                            @Nullable String altitude) {
        disableWatchers = true;
        latitudeInput.setText(latitude);
        longitudeInput.setText(longitude);
        if (altitude != null) {
            elevationInput.setText(altitude);
        } else {
            elevationInput.setText("");
        }
        disableWatchers = false;
    }

    void showCoordinatesAccurate() {
        statusIndicator.setTextColor(Color.GREEN);
        accuracyWarning.setVisibility(GONE);
    }

    void showCoordinatesInaccurate() {
        statusIndicator.setTextColor(Color.RED);
        if (hasLocation()) {
            accuracyWarning.setVisibility(VISIBLE);
        } else {
            accuracyWarning.setVisibility(GONE);
        }
    }

    void showLocationListenerStopped() {
        setAlpha(ALPHA_OPAQUE);
        enableManualInput();
    }

    private void enableManualInput() {
        latitudeInput.setEnabled(true);
        longitudeInput.setEnabled(true);
        elevationInput.setEnabled(true);
    }

    void showLocationListenerStarted() {
        setAlpha(ALPHA_TRANSPARENT);
        resetChildViewsToDefaultValues();
        showCoordinatesInaccurate();
        disableManualInput();
    }

    private void disableManualInput() {
        latitudeInput.setEnabled(false);
        longitudeInput.setEnabled(false);
        elevationInput.setEnabled(false);
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

    public boolean hasLocation() {
        return !TextUtils.isEmpty(latitudeInput.getText().toString()) &&
                !TextUtils.isEmpty(longitudeInput.getText().toString());
    }

    private static class GeoInputTextWatcher implements TextWatcher {

        private final GeoInputTextWatcherListener geoInputTextWatcherListener;

        private GeoInputTextWatcher(GeoInputTextWatcherListener geoInputTextWatcherListener) {
            this.geoInputTextWatcherListener = geoInputTextWatcherListener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            //EMPTY
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //EMPTY
        }

        @Override
        public void afterTextChanged(Editable s) {
            geoInputTextWatcherListener.validateCoordinate();
        }
    }

    interface GeoInputTextWatcherListener {

        void validateCoordinate();
    }
}
