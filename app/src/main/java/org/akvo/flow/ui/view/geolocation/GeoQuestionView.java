/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.presentation.PermissionRationaleDialogFragment;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.ui.fragment.GpsDisabledDialogFragment;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.LocationValidator;
import org.akvo.flow.util.PlatformUtil;

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
    private static final int POSITION_LATITUDE = 0;
    private static final int POSITION_LONGITUDE = 1;
    private static final int POSITION_ALTITUDE = 2;

    private final TimedLocationListener mLocationListener;
    private final LocationValidator locationValidator = new LocationValidator();
    private final SnackBarManager snackBarManager = new SnackBarManager();

    private Button mGeoButton;
    private View geoLoading;
    private GeoInputContainer geoInputContainer;

    private float mLastAccuracy;

    public GeoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new TimedLocationListener(context, this, allowMockLocations(q));
        init();
    }

    private boolean allowMockLocations(Question q) {
        return !q.isLocked() || PlatformUtil.isEmulator();
    }

    private void init() {
        setQuestionView(R.layout.geo_question_view);
        setId(R.id.geo_question_view);
        mGeoButton = findViewById(R.id.geo_btn);
        geoLoading = findViewById(R.id.auto_geo_location_progress);
        geoInputContainer = findViewById(R.id.manual_geo_input_container);

        geoInputContainer.setTextWatchers(this);
        mGeoButton.setOnClickListener(this);

        if (isReadOnly()) {
            geoInputContainer.disableManualInputs();
            mGeoButton.setVisibility(View.GONE);
        }
        if (mQuestion.isLocked()) {
            geoInputContainer.disableManualInputs();
        }
    }

    public void onClick(View v) {
        if (mLocationListener.isListening()) {
            stopLocationListener();
        } else {
            if (TextUtils.isEmpty(geoInputContainer.getLatitudeText()) || TextUtils
                    .isEmpty(geoInputContainer.getLongitudeText())) {
                startListeningToLocation();
            } else {
                displayConfirmResetFields();
            }
        }
    }

    public void startListeningToLocation() {
        resetQuestion(true);
        showLocationListenerStarted();
        resetAccuracy();
        startLocation();
    }

    private void displayConfirmResetFields() {
        Context context = getContext();
        if (context instanceof AppCompatActivity) {
            FragmentManager fragmentManager = ((AppCompatActivity) context)
                    .getSupportFragmentManager();
            DialogFragment newFragment = GeoFieldsResetConfirmDialogFragment
                    .newInstance(getQuestion().getId());
            newFragment.show(fragmentManager, GeoFieldsResetConfirmDialogFragment.GEO_DIALOG_TAG);
        }
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

    private void resetAccuracy() {
        mLastAccuracy = UNKNOWN_ACCURACY;
    }

    private void startLocation() {
        if (isLocationPermissionGranted()) {
            mLocationListener.start();
        } else {
            FormActivity activity = (FormActivity) getContext();
            String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
                String questionId = getQuestion().getQuestionId();
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                DialogFragment fragment = PermissionRationaleDialogFragment
                        .newInstance(permissions, ConstantUtil.LOCATION_PERMISSION_CODE, questionId);
                fragment.show(activity.getSupportFragmentManager(),
                        PermissionRationaleDialogFragment.TAG);
            } else {
                activity.requestPermissions(permissions, ConstantUtil.LOCATION_PERMISSION_CODE,
                        questionId);
            }
        }
    }

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; ++i) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mLocationListener.start();
                } else {
                    View coordinatorLayout = getRootView().findViewById(R.id.coordinator_layout);
                    snackBarManager.displaySnackBarWithAction(coordinatorLayout,
                            R.string.location_permission_refused,
                            R.string.action_retry,
                            new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startLocation();
                                }
                            }, getContext());
                }
                break;
            }
        }
    }

    private void stopLocation() {
        mLocationListener.stop();
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

    @Override
    public void onQuestionResultReceived(Bundle data) {
        //EMPTY
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        resetAccuracy();
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
    }


    @Override
    public void onTimeout() {
        showLocationListenerStopped();
        View coordinatorLayout = getRootView().findViewById(R.id.coordinator_layout);
        snackBarManager.displaySnackBarWithAction(coordinatorLayout, R.string.location_timeout,
                R.string.action_retry,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetAccuracy();
                        startLocation();
                        showLocationListenerStarted();
                    }
                }, getContext());
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
            setGeoQuestionResponse(lat, lon);
        } else {
            setResponse(null);
        }
    }

    private void setResponse() {
        final String lat = geoInputContainer.getLatitudeText();
        final String lon = geoInputContainer.getLongitudeText();

        if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lon)) {
            setResponse(null);
        } else {
            setGeoQuestionResponse(lat, lon);
        }
    }

    private void setGeoQuestionResponse(String lat, String lon) {
        QuestionResponse questionResponse = new QuestionResponse.QuestionResponseBuilder()
                .setValue(getResponse(lat, lon))
                .setType(ConstantUtil.GEO_RESPONSE_TYPE)
                .setQuestionId(getQuestion().getId())
                .createQuestionResponse();
        setResponse(questionResponse);
    }

    @NonNull
    private String getResponse(String lat, String lon) {
        return lat + RESPONSE_DELIMITER + lon + RESPONSE_DELIMITER + geoInputContainer
                .getElevationText();
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

    @Override
    public boolean isValid() {
        final String lat = geoInputContainer.getLatitudeText();
        final String lon = geoInputContainer.getLongitudeText();
        if (!super.isValid() || !locationValidator.validCoordinates(lat, lon)) {
            setError(getResources().getString(R.string.error_question_mandatory));
            return false;
        }
        return true;
    }
}
