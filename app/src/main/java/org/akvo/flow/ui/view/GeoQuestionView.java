/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

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
    private static final String DELIM = "|";
    private Button mGeoButton;
    private EditText mLatField;
    private EditText mLonField;
    private EditText mElevationField;
    private TextView mStatusIndicator;
    private TextView mSearchingIndicator;
    private String mCode = "";
    private float mLastAccuracy;
    private TimedLocationListener mLocationListener;

    public GeoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new TimedLocationListener(context, this, !q.isLocked());
        init();
    }

    private void init() {
        setQuestionView(R.layout.geo_question_view);

        mLatField = (EditText)findViewById(R.id.lat_et);
        mLonField = (EditText)findViewById(R.id.lon_et);
        mElevationField = (EditText)findViewById(R.id.height_et);
        mGeoButton = (Button)findViewById(R.id.geo_btn);
        mSearchingIndicator = (TextView)findViewById(R.id.searching_tv);
        mStatusIndicator = (TextView)findViewById(R.id.acc_tv);

        mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": ");
        mSearchingIndicator.setText("");

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
        mSearchingIndicator.setText(R.string.searching);
        mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": ");
        mStatusIndicator.setTextColor(Color.RED);

        mLatField.setText("");
        mLonField.setText("");
        mElevationField.setText("");
        mCode = "";
        mLastAccuracy = UNKNOWN_ACCURACY;
        mLocationListener.start();
    }

    /**
     * generates a unique code based on the lat/lon passed in. Current algorithm
     * returns the concatenation of the integer portion of 1000 times absolute
     * value of lat and lon in base 36
     * 
     * @param lat
     * @param lon
     * @return
     */
    private String generateCode(double lat, double lon) {
        try {
            Long code = Long.parseLong((int) ((Math.abs(lat) * 100000d)) + ""
                    + (int) ((Math.abs(lon) * 10000d)));
            return Long.toString(code, 36);
        } catch (NumberFormatException e) {
            Timber.e("Code cannot be generated: " + e.getMessage());
            return "";
        }
    }

    /**
     * clears out the UI fields
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mLatField.setText("");
        mLonField.setText("");
        mElevationField.setText("");
        mCode = "";
        mSearchingIndicator.setText("");
        mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": ");
    }

    /**
     * restores the file path for the file and turns on the complete icon if the
     * file exists
     */
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
    }

    @Override
    public void onLocationReady(double latitude, double longitude, double altitude, float accuracy) {
        if (accuracy < mLastAccuracy) {
            mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": "
                    + new DecimalFormat("#").format(accuracy) + "m");
            mLatField.setText(latitude + "");
            mLonField.setText(longitude + "");
            // elevation is in meters, even one decimal is way more than GPS precision
            mElevationField.setText(new DecimalFormat("#.#").format(altitude));
            mCode = generateCode(latitude, longitude);
        }
        if (accuracy <= TimedLocationListener.ACCURACY_DEFAULT) {
            mLocationListener.stop();
            setResponse();
            mSearchingIndicator.setText(R.string.ready);
            mStatusIndicator.setTextColor(Color.GREEN);
        }
    }

    @Override
    public void onTimeout() {
        // Unknown location
        resetQuestion(true);
        mSearchingIndicator.setText(R.string.timeout);
    }

    @Override
    public void onGPSDisabled() {
        // we can't turn GPS on directly, the best we can do is launch the settings page
        ViewUtil.showGPSDialog(getContext());
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
                mCode = generateCode(Double.parseDouble(mLatField.getText().toString()),
                        Double.parseDouble(mLonField.getText().toString()));
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
            setResponse(new QuestionResponse(lat + DELIM + lon + DELIM + mElevationField.getText()
                    + DELIM + mCode,
                    ConstantUtil.GEO_RESPONSE_TYPE, getQuestion().getId()));
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }

}
