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

package org.akvo.flow.presentation.datapoints.list;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.util.GeoUtil;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

class DataPointListAdapter extends BaseAdapter {

    private Double latitude;
    private Double longitude;
    private final LayoutInflater inflater;
    private final String dataLabel;
    private final List<ListDataPoint> dataPoints;
    private final GeoUtil geoUtil;

    DataPointListAdapter(Context context, @Nullable Double latitude,
            @Nullable Double longitude, SurveyGroup surveyGroup) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.inflater = LayoutInflater.from(context);
        this.dataLabel = context.getString(getDateLabel(surveyGroup));
        dataPoints = new ArrayList<>();
        geoUtil = new GeoUtil();
    }

    @StringRes
    private int getDateLabel(SurveyGroup surveyGroup) {
        if (surveyGroup != null && surveyGroup.isMonitored()) {
            return R.string.last_modified_monitored;
        } else {
            return R.string.last_modified_regular;
        }
    }

    @Override
    public int getCount() {
        return dataPoints.size();
    }

    @NonNull
    @Override
    public ListDataPoint getItem(int position) {
        return dataPoints.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.datapoint_list_item, parent, false);
        } else {
            view = convertView;
        }
        TextView nameView = view.findViewById(R.id.locale_name);
        TextView idView = view.findViewById(R.id.locale_id);
        TextView dateView = view.findViewById(R.id.last_modified);
        TextView distanceView = view.findViewById(R.id.locale_distance);
        TextView statusView = view.findViewById(R.id.status);
        ImageView statusImage = view.findViewById(R.id.status_img);

        final ListDataPoint dataPoint = getItem(position);
        Context context = parent.getContext();
        int status = dataPoint.getStatus();
        nameView.setText(dataPoint.getDisplayName());
        idView.setText(dataPoint.getId());

        displayDistanceText(distanceView, getDistanceText(dataPoint, context));
        displayDateText(dateView, dataPoint.getLastModified());

        int statusRes = 0;
        String statusText = null;
        switch (status) {
            case SurveyInstanceStatus.SAVED:
            case SurveyInstanceStatus.SUBMIT_REQUESTED:
                statusRes = R.drawable.record_saved_icn;
                statusText = context.getString(R.string.status_saved);
                break;
            case SurveyInstanceStatus.SUBMITTED:
                statusRes = R.drawable.record_submitted_icn;
                statusText = context.getString(R.string.status_submitted);
                break;
            case SurveyInstanceStatus.UPLOADED:
            case SurveyInstanceStatus.DOWNLOADED:
                statusRes = R.drawable.record_synced_icn;
                statusText = context.getString(R.string.status_uploaded);
                break;
            default:
                //wrong state
                break;
        }

        statusImage.setImageResource(statusRes);
        statusView.setText(statusText);

        if (dataPoint.wasViewed()) {
            nameView.setTypeface(null, Typeface.NORMAL);
            idView.setTypeface(null, Typeface.NORMAL);
            dateView.setTypeface(null, Typeface.NORMAL);
            distanceView.setTypeface(null, Typeface.NORMAL);
            statusView.setTypeface(null, Typeface.NORMAL);
        } else {
            nameView.setTypeface(null, Typeface.BOLD);
            idView.setTypeface(null, Typeface.BOLD);
            dateView.setTypeface(null, Typeface.BOLD);
            distanceView.setTypeface(null, Typeface.BOLD);
            statusView.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private String getDistanceText(@NonNull ListDataPoint dataPoint, Context context) {
        StringBuilder builder = new StringBuilder(
                context.getString(R.string.distance_label) + " ");

        if (latitude != null && longitude != null && dataPoint.isLocationValid()) {
            float[] results = new float[1];
            Location.distanceBetween(latitude, longitude, dataPoint.getLatitude(),
                    dataPoint.getLongitude(), results);
            final double distance = results[0];

            builder.append(geoUtil.getDisplayLength(distance));
            return builder.toString();
        }

        return null;
    }

    private void displayDateText(TextView tv, Long time) {
        if (time != null && time > 0) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(dataLabel + " " + new PrettyTime().format(new Date(time)));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void displayDistanceText(TextView tv, String distance) {
        if (!TextUtils.isEmpty(distance)) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(distance);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    void setDataPoints(List<ListDataPoint> dataPoints) {
        this.dataPoints.clear();
        this.dataPoints.addAll(dataPoints);
        notifyDataSetChanged();
    }

    void updateLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
