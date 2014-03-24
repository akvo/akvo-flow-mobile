/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

/**
 * Question that can handle geographic location input. This question can also
 * listen to location updates from the GPS sensor on the device.
 * 
 * @author Christopher Fagiani
 */
public class GeoQuestionView extends QuestionView implements OnClickListener,
        LocationListener, OnFocusChangeListener {
    private static final float UNKNOWN_ACCURACY = 99999999f;
    private static final float ACCURACY_THRESHOLD = 25f;
    private static final String DELIM = "|";
    private Button mGeoButton;
    private EditText mLatField;
    private EditText mLonField;
    private EditText mElevationField;
    private TextView mStatusIndicator;
    private TextView mSearchingIndicator;
    private String mCode = "";
    private float mLastAccuracy;
    private boolean mNeedUpdate = false;

    public GeoQuestionView(Context context, Question q, String defaultLang, String[] langCodes,
            boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        // TODO: parameterize (add to Question ?)
        init();
    }

    protected void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.barcode_question_view, this, true);

        setupQuestion();

        mLatField = (EditText)findViewById(R.id.lat_et);
        mLonField = (EditText)findViewById(R.id.lon_et);
        mElevationField = (EditText)findViewById(R.id.height_et);
        mGeoButton = (Button)findViewById(R.id.geo_btn);
        mSearchingIndicator = (TextView)findViewById(R.id.searching_tv);
        mStatusIndicator = (TextView)findViewById(R.id.acc_tv);

        mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": ");
        mStatusIndicator.setTextColor(Color.WHITE);
        mSearchingIndicator.setText("");
        mSearchingIndicator.setTextColor(Color.WHITE);

        mLatField.setOnFocusChangeListener(this);
        mLonField.setOnFocusChangeListener(this);
        mElevationField.setOnFocusChangeListener(this);
        mGeoButton.setOnClickListener(this);

        if (mReadOnly) {
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
     * When the user clicks the "Populate Geo" button, start listening for
     * location updates
     */
    public void onClick(View v) {
        LocationManager locMgr = (LocationManager) getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": ");
            mStatusIndicator.setTextColor(Color.WHITE);

            mLatField.setText("");
            mLonField.setText("");
            mElevationField.setText("");
            mCode = "";
            mNeedUpdate = true;
            mSearchingIndicator.setText(R.string.searching);
            mLastAccuracy = UNKNOWN_ACCURACY;
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            // we can't turn GPS on directly, the best we can do is launch the
            // settings page
            ViewUtil.showGPSDialog(getContext());
        }
    }

    /**
     * populates the fields on the UI with the location info from the event
     * 
     * @param loc
     */
    private void populateLocation(Location loc) {
        if (loc.hasAccuracy()) {
            mStatusIndicator.setText(getContext().getString(R.string.accuracy) + ": "
                    + new DecimalFormat("#").format(loc.getAccuracy()) + "m");
            if (loc.getAccuracy() <= ACCURACY_THRESHOLD) {
                mStatusIndicator.setTextColor(Color.GREEN);
            } else {
                mStatusIndicator.setTextColor(Color.RED);
            }
        }
        mLatField.setText(loc.getLatitude() + "");
        mLonField.setText(loc.getLongitude() + "");
        // elevation is in meters, even one decimal is way more than GPS
        // precision
        mElevationField.setText(new DecimalFormat("#.#").format(loc.getAltitude()));
        mCode = generateCode(loc.getLatitude(), loc.getLongitude());
        setResponse();
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
        Long code = Long.parseLong((int) ((Math.abs(lat) * 100000d)) + ""
                + (int) ((Math.abs(lon) * 10000d)));
        return Long.toString(code, 36);
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
        mStatusIndicator.setTextColor(Color.WHITE);
    }

    /**
     * restores the file path for the file and turns on the complete icon if the
     * file exists
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null && resp.getValue() != null) {
            StringTokenizer strTok = new StringTokenizer(resp.getValue(), DELIM);
            if (strTok.countTokens() >= 3) {
                mLatField.setText(strTok.nextToken());
                mLonField.setText(strTok.nextToken());
                mElevationField.setText(strTok.nextToken());
                if (strTok.hasMoreTokens()) {
                    mCode = strTok.nextToken();
                }
            }
        }
    }

    @Override
    public void questionComplete(Bundle data) {
        // completeIcon.setVisibility(View.VISIBLE);
    }

    /**
     * called by the system when it gets location updates.
     */
    public void onLocationChanged(Location location) {
        float currentAccuracy = location.getAccuracy();
        // if accuracy is 0 then the gps has no idea where we're at
        if (currentAccuracy > 0) {

            // If we are below the accuracy treshold, stop listening for
            // updates.
            // This means that after the geolocation is 'green', it stays the
            // same,
            // otherwise it keeps on listening
            if (currentAccuracy <= ACCURACY_THRESHOLD) {
                LocationManager locMgr = (LocationManager) getContext()
                        .getSystemService(Context.LOCATION_SERVICE);
                locMgr.removeUpdates(this);
                mSearchingIndicator.setText(R.string.ready);
            }

            // if the location reading is more accurate than the last, update
            // the view
            if (mLastAccuracy > currentAccuracy || mNeedUpdate) {
                mLastAccuracy = currentAccuracy;
                mNeedUpdate = false;
                populateLocation(location);
            }
        } else if (mNeedUpdate) {
            mNeedUpdate = true;
            populateLocation(location);
        }
    }

    public void onProviderDisabled(String provider) {
        // no op. needed for LocationListener interface
    }

    public void onProviderEnabled(String provider) {
        // no op. needed for LocationListener interface
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // no op. needed for LocationListener interface
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
    public void releaseResources() {
        // Remove updates from LocationManager, to allow this object being GC
        // and avoid an unnecessary use of the GPS and battery draining.
        LocationManager locMgr = (LocationManager) getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        locMgr.removeUpdates(this);

        // Update the UI in case we come back later to the same instance
        mSearchingIndicator.setText("");
    }

}
