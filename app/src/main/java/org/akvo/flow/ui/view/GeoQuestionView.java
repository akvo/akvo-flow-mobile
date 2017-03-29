/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.ui.fragment.GpsDisabledDialogFragment;
import org.akvo.flow.util.ConstantUtil;

import java.text.DecimalFormat;

import timber.log.Timber;

/**
 * Question that can handle geographic location input. This question can also
 * listen to location updates from the GPS sensor on the device.
 *
 * @author Christopher Fagiani
 */
public class GeoQuestionView extends QuestionView implements OnClickListener, OnFocusChangeListener,
        TimedLocationListener.Listener {

    private static final float UNKNOWN_ACCURACY = 99999999f;
    private static final String RESPONSE_DELIMITER = "|";
    private static final int SNACK_BAR_DURATION_IN_MS = 2000;

    private final TimedLocationListener mLocationListener;
    private final DecimalFormat accuracyFormat = new DecimalFormat("#");
    private final DecimalFormat altitudeFormat = new DecimalFormat("#.#");

    private EditText mLatField;
    private EditText mLonField;
    private EditText mElevationField;
    private TextView mStatusIndicator;

    private String mCode = "";
    private float mLastAccuracy;

    public GeoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new TimedLocationListener(context, this, !q.isLocked());
        init();
    }

    private void init() {
        setQuestionView(R.layout.geo_question_view);

        mLatField = (EditText) findViewById(R.id.lat_et);
        mLonField = (EditText) findViewById(R.id.lon_et);
        mElevationField = (EditText) findViewById(R.id.height_et);
        Button mGeoButton = (Button) findViewById(R.id.geo_btn);
        mStatusIndicator = (TextView) findViewById(R.id.acc_tv);

        mStatusIndicator.setText(R.string.geo_location_accuracy_default);

        mLatField.setOnFocusChangeListener(this);
        mLonField.setOnFocusChangeListener(this);
        mElevationField.setOnFocusChangeListener(this);
        mGeoButton.setOnClickListener(this);

        if (isReadOnly()) {
            mLatField.setFocusable(false);
            mLonField.setFocusable(false);
            mElevationField.setFocusable(false);
            mGeoButton.setEnabled(false);
        }
        if (mQuestion.isLocked()) {
            mLatField.setFocusable(false);
            mLonField.setFocusable(false);
            mElevationField.setFocusable(false);
        }
    }

    /**
     * When the user clicks the "Populate Geo" button, start listening for location updates
     */
    public void onClick(View v) {
        resetViewsToDefaultValues();
        setStatusToRed();
        resetResponseValues();
        startLocation();
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

    private void resetViewsToDefaultValues() {
        mStatusIndicator.setText(R.string.geo_location_accuracy_default);
        mLatField.setText("");
        mLonField.setText("");
        mElevationField.setText("");
    }

    private void setStatusToRed() {
        mStatusIndicator.setTextColor(Color.RED);
    }

    private void setStatusToGreen() {
        mStatusIndicator.setTextColor(Color.GREEN);
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

    /**
     * clears out the UI fields
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        resetViewsToDefaultValues();
        resetCode();
        stopLocation();
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null && resp.getValue() != null) {
            String[] tokens = resp.getValue().split("\\|", -1);
            if (tokens.length > 2) {
                mLatField.setText(tokens[0]);
                mLonField.setText(tokens[1]);
                mElevationField.setText(tokens[2]);
                if (tokens.length > 3) {
                    mCode = tokens[3];
                }
            }
        }
    }

    @Override
    public void questionComplete(Bundle data) {
        //EMPTY
    }

    @Override
    public void onLocationReady(double latitude, double longitude, double altitude,
            float accuracy) {
        if (accuracy < mLastAccuracy) {
            updateViews(latitude, longitude, altitude, accuracy);
            updateCode(latitude, longitude);
        }
        if (accuracy <= TimedLocationListener.ACCURACY_DEFAULT) {
            stopLocation();
            setResponse();
            setStatusToGreen();
        }
    }

    private void updateCode(double latitude, double longitude) {
        mCode = generateCode(latitude, longitude);
    }

    private void updateViews(double latitude, double longitude, double altitude, float accuracy) {
        mStatusIndicator.setText(getContext()
                .getString(R.string.geo_location_accuracy, accuracyFormat.format(accuracy)));
        mLatField.setText(latitude + "");
        mLonField.setText(longitude + "");
        mElevationField.setText(altitudeFormat.format(altitude));
    }

    @Override
    public void onTimeout() {
        resetQuestion(true);
        Snackbar.make(this, R.string.location_timeout, SNACK_BAR_DURATION_IN_MS)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetViewsToDefaultValues();
                        setStatusToRed();
                        resetResponseValues();
                        startLocation();
                    }
                }).show();
    }

    @Override
    public void onGPSDisabled() {
        Context context = getContext();
        if (context instanceof AppCompatActivity) {
            FragmentManager fragmentManager = ((AppCompatActivity) context)
                    .getSupportFragmentManager();
            DialogFragment newFragment = GpsDisabledDialogFragment.newInstance();
            newFragment.show(fragmentManager, GpsDisabledDialogFragment.GPS_DIALOG_TAG);
        }
    }

    /**
     * used to capture lat/lon/elevation if manually typed
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            final String lat = mLatField.getText().toString();
            final String lon = mLonField.getText().toString();
            if (!TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lon)) {
                updateCode(Double.parseDouble(lat), Double.parseDouble(lon));
            }
            setResponse();
        }
    }

    private void setResponse() {
        final String lat = mLatField.getText().toString();
        final String lon = mLonField.getText().toString();

        if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lon)) {
            setResponse(null);
        } else {
            setResponse(new QuestionResponse(getResponse(lat, lon), ConstantUtil.GEO_RESPONSE_TYPE,
                    getQuestion().getId()));
        }
    }

    @NonNull
    private String getResponse(String lat, String lon) {
        return lat + RESPONSE_DELIMITER + lon + RESPONSE_DELIMITER + mElevationField.getText()
                + RESPONSE_DELIMITER + mCode;
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        //EMPTY
    }
}
