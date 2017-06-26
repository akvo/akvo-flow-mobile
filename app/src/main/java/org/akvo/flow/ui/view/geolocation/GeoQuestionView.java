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

package org.akvo.flow.ui.view.geolocation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.ui.fragment.GpsDisabledDialogFragment;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.LocationValidator;

import timber.log.Timber;

/**
 * Question that can handle geographic location input. This question can also
 * listen to location updates from the GPS sensor on the device.
 *
 * @author Christopher Fagiani
 */
public class GeoQuestionView extends QuestionView
        implements OnClickListener, TimedLocationListener.Listener {

    private static final float UNKNOWN_ACCURACY = 99999999f;
    private static final String RESPONSE_DELIMITER = "|";
    private static final int SNACK_BAR_DURATION_IN_MS = 4000;
    public static final int POSITION_LATITUDE = 0;
    public static final int POSITION_LONGITUDE = 1;
    private static final int POSITION_ALTITUDE = 2;
    private static final int POSITION_CODE = 3;

    private final TimedLocationListener mLocationListener;
    private final LocationValidator locationValidator = new LocationValidator();

    private Button mGeoButton;
    private View geoLoading;
    private GeoInputContainer geoInputContainer;

    private String mCode = "";
    private float mLastAccuracy;

    public GeoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new TimedLocationListener(context, this, !q.isLocked());
        init();
    }

    private void init() {
        setQuestionView(R.layout.geo_question_view);

        mGeoButton = (Button) findViewById(R.id.geo_btn);
        geoLoading = findViewById(R.id.auto_geo_location_progress);
        geoInputContainer = (GeoInputContainer) findViewById(R.id.manual_geo_input_container);

        geoInputContainer.setTextWatchers(this);
        mGeoButton.setOnClickListener(this);

        if (isReadOnly()) {
            geoInputContainer.disableInputsFocusability();
            mGeoButton.setEnabled(false);
        }
        if (mQuestion.isLocked()) {
            geoInputContainer.disableInputsFocusability();
        }
    }

    public void onClick(View v) {
        if (mLocationListener.isListening()) {
            stopLocationListener();
        } else {
            startListeningToLocation();
        }
    }

    private void startListeningToLocation() {
        resetQuestion(true);
        showLocationListenerStarted();
        resetResponseValues();
        startLocation();
    }

    private void showLocationListenerStarted() {
        geoLoading.setVisibility(VISIBLE);
        geoInputContainer.showLocationListenerStarted();
        updateButtonTextToCancel();
    }

    private void stopLocationListener() {
        stopLocation();
        showLocationListenerStopped();
    }

    private void showLocationListenerStopped() {
        geoLoading.setVisibility(GONE);
        geoInputContainer.showLocationListenerStopped();
        updateButtonTextToGetGeo();
    }

    private void updateButtonTextToGetGeo() {
        mGeoButton.setText(R.string.getgeo);
    }

    private void updateButtonTextToCancel() {
        mGeoButton.setText(R.string.cancelbutton);
    }

    private void resetResponseValues() {
        resetCode();
        resetAccuracy();
    }

    private void resetAccuracy() {
        mLastAccuracy = UNKNOWN_ACCURACY;
    }

    private void resetCode() {
        mCode = "";
    }

    private void startLocation() {
        mLocationListener.start();
    }

    private void stopLocation() {
        mLocationListener.stop();
    }

    /**
     * generates a unique code based on the lat/lon passed in. Current algorithm
     * returns the concatenation of the integer portion of 1000 times absolute
     * value of lat and lon in base 36
     */
    private String generateCode(double lat, double lon) {
        try {
            Long code = Long.parseLong((int) ((Math.abs(lat) * 100000d)) + ""
                    + (int) ((Math.abs(lon) * 10000d)));
            return Long.toString(code, 36);
        } catch (NumberFormatException e) {
            Timber.e(e, "Location response code cannot be generated: %s", e.getMessage());
            return "";
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null && resp.getValue() != null) {
            String[] tokens = resp.getValue().split("\\|", -1);
            String latitude = getLatitudeFromResponseToken(tokens);
            String longitude = getLongitudeFromToken(tokens);
            String altitude = getAltitudeFromToken(tokens);
            geoInputContainer.displayCoordinates(latitude, longitude, altitude);
            mCode = getCodeFromToken(tokens);
        }
    }

    @NonNull
    private String getLatitudeFromResponseToken(@Nullable String[] token) {
        if (token == null || token.length == 0) {
            return "";
        }
        return token[POSITION_LATITUDE];
    }

    @NonNull
    private String getLongitudeFromToken(String[] token) {
        if (token == null || token.length <= POSITION_LONGITUDE) {
            return "";
        }
        return token[POSITION_LONGITUDE];
    }

    @NonNull
    private String getAltitudeFromToken(String[] token) {
        if (token == null || token.length <= POSITION_ALTITUDE) {
            return "";
        }
        return token[POSITION_ALTITUDE];
    }

    @NonNull
    private String getCodeFromToken(String[] token) {
        if (token == null || token.length <= POSITION_CODE) {
            return "";
        }
        return token[POSITION_CODE];
    }

    @Override
    public void questionComplete(Bundle data) {
        //EMPTY
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        resetResponseValues();
        geoInputContainer.displayCoordinates("", "", "");
    }

    @Override
    public void onLocationReady(double latitude, double longitude, double altitude,
            float accuracy) {
        boolean areNewCoordinatesMoreAccurate = accuracy < mLastAccuracy;
        if (areNewCoordinatesMoreAccurate) {
            updateWithNewCoordinates(latitude, longitude, altitude, accuracy);
        }
        boolean areNewCoordinatesAccurateEnough =
                accuracy <= TimedLocationListener.ACCURACY_DEFAULT;
        if (areNewCoordinatesAccurateEnough) {
            useAccurateCoordinates();
        }
    }

    private void useAccurateCoordinates() {
        stopLocation();
        setResponse();
        geoInputContainer.showCoordinatesAccurate();
        showLocationListenerStopped();
    }

    private void updateWithNewCoordinates(double latitude, double longitude, double altitude,
            float accuracy) {
        geoInputContainer
                .displayCoordinates(latitude + "", longitude + "", altitude + "", accuracy);
        updateCode(latitude, longitude);
    }

    private void updateCode(double latitude, double longitude) {
        mCode = generateCode(latitude, longitude);
    }

    @Override
    public void onTimeout() {
        showLocationListenerStopped();
        Snackbar.make(this, R.string.location_timeout, SNACK_BAR_DURATION_IN_MS)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetResponseValues();
                        startLocation();
                        showLocationListenerStarted();
                    }
                }).show();
    }

    @Override
    public void onGPSDisabled() {
        Context context = getContext();
        showLocationListenerStopped();
        if (context instanceof AppCompatActivity) {
            FragmentManager fragmentManager = ((AppCompatActivity) context)
                    .getSupportFragmentManager();
            DialogFragment newFragment = GpsDisabledDialogFragment.newInstance();
            newFragment.show(fragmentManager, GpsDisabledDialogFragment.GPS_DIALOG_TAG);
        }
    }

    private void saveManualFields() {
        final String lat = geoInputContainer.getLatitudeText();
        final String lon = geoInputContainer.getLongitudeText();
        if (locationValidator.validCoordinates(lat, lon)) {
            updateCode(Double.parseDouble(lat), Double.parseDouble(lon));
            setResponse(new QuestionResponse(getResponse(lat, lon), ConstantUtil.GEO_RESPONSE_TYPE,
                    getQuestion().getId()));
        } else {
            resetCode();
            setResponse(null);
        }
    }

    private void setResponse() {
        final String lat = geoInputContainer.getLatitudeText();
        final String lon = geoInputContainer.getLongitudeText();

        if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lon)) {
            setResponse(null);
        } else {
            setResponse(new QuestionResponse(getResponse(lat, lon), ConstantUtil.GEO_RESPONSE_TYPE,
                    getQuestion().getId()));
        }
    }

    @NonNull
    private String getResponse(String lat, String lon) {
        return lat + RESPONSE_DELIMITER + lon + RESPONSE_DELIMITER + geoInputContainer
                .getElevationText() + RESPONSE_DELIMITER + mCode;
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        saveManualFields();
    }

    @Override
    public void onDestroy() {
        if (mLocationListener != null && mLocationListener.isListening()) {
            mLocationListener.stop();
        }
    }
}
