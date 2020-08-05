/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.PermissionAwareLocationListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.view.LocationSnackBarManager;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.LocationValidator;
import org.akvo.flow.util.PlatformUtil;

import javax.inject.Inject;

/**
 * Question that can handle geographic location input. This question can also
 * listen to location updates from the GPS sensor on the device.
 *
 * @author Christopher Fagiani
 */
public class GeoQuestionView extends QuestionView
        implements OnClickListener, TimedLocationListener.Listener,
        PermissionAwareLocationListener.PermissionListener {

    private static final String RESPONSE_DELIMITER = "|";
    private static final int POSITION_LATITUDE = 0;
    private static final int POSITION_LONGITUDE = 1;
    private static final int POSITION_ALTITUDE = 2;

    @Inject
    LocationSnackBarManager locationSnackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    LocationValidator locationValidator;

    private final PermissionAwareLocationListener mLocationListener;

    private Button mGeoButton;
    private View geoLoading;
    private GeoInputContainer geoInputContainer;

    public GeoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new PermissionAwareLocationListener(context, this,
                allowMockLocations(q), q.getQuestionId(), this);
        init();
    }

    private boolean allowMockLocations(Question q) {
        return !q.isLocked() || PlatformUtil.isEmulator();
    }

    private void init() {
        setQuestionView(R.layout.geo_question_view);
        initialiseInjector();
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

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
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
        mLocationListener.startLocationIfPossible();
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
        mLocationListener.stopLocation();
        showLocationListenerStopped();
    }

    private void showLocationListenerStopped() {
        geoLoading.setVisibility(GONE);
        geoInputContainer.showLocationListenerStopped();
        updateButtonTextToGetGeo();
    }

    private void updateButtonTextToGetGeo() {
        if (geoInputContainer.hasLocation()) {
            mGeoButton.setText(R.string.updategeo);
        } else {
            mGeoButton.setText(R.string.getgeo);
        }
    }

    private void updateButtonTextToCancel() {
        mGeoButton.setText(R.string.cancelbutton);
    }

    private void resetAccuracy() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mLocationListener.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionNotGranted() {
        stopLocationListener();
        View coordinatorLayout = getRootView().findViewById(R.id.coordinator_layout);
        locationSnackBarManager
                .displayPermissionMissingSnackBar(coordinatorLayout,
                        v -> startListeningToLocation(),
                        getContext());
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
        mLocationListener.stopLocation();
        showLocationListenerStopped();
        updateWithNewCoordinates(latitude, longitude, altitude, accuracy);
        boolean accurate = accuracy <= TimedLocationListener.ACCURACY_DEFAULT;
        if (accurate) {
            geoInputContainer.showCoordinatesAccurate();
        } else {
            geoInputContainer.showCoordinatesInaccurate();
        }
        updateButtonTextToGetGeo();
        setResponse();
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
        locationSnackBarManager.displayLocationTimeoutSnackBar(coordinatorLayout, v -> {
            resetAccuracy();
            mLocationListener.startLocationIfPossible();
            showLocationListenerStarted();
        }, getContext());
    }

    @Override
    public void onGPSDisabled() {
        Context context = getContext();
        showLocationListenerStopped();
        if (context instanceof AppCompatActivity) {
            View coordinatorLayout = getRootView().findViewById(R.id.coordinator_layout);
            locationSnackBarManager.displayGeoLocationDiabled(coordinatorLayout, v ->
                    navigator.navigateToLocationSettings(context), context);
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
        setResponse(getQuestion(), getResponse(lat, lon), ConstantUtil.GEO_RESPONSE_TYPE);
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
        if (mLocationListener != null) {
            mLocationListener.stopLocation();
        }
    }

    @Override
    public boolean isValid() {
        if (getQuestion().isMandatory()) {
//            String[] tokens = getResponse().getValue().split("\\|", -1);
//            String latitude = getLatitudeFromResponseToken(tokens);
//            String longitude = getLongitudeFromToken(tokens);
            final String lat = geoInputContainer.getLatitudeText();
            final String lon = geoInputContainer.getLongitudeText();
            if (!super.isValid() || !locationValidator.validCoordinates(lat, lon)) {
                setError(getResources().getString(R.string.error_question_mandatory));
                return false;
            }
        }
        return true;
    }
}
