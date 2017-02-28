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

package org.akvo.flow.presentation.datapoints.list;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.util.GeoUtil;
import org.akvo.flow.util.PlatformUtil;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;

/**
 * List Adapter to bind the Surveyed Locales into the list items
 */
class SurveyedLocaleListAdapter extends ArrayAdapter<ListDataPoint> {

    private Double mLatitude;
    private Double mLongitude;
    private final SurveyGroup mSurveyGroup;
    private final LayoutInflater inflater;

    SurveyedLocaleListAdapter(Context context, @Nullable Double mLatitude,
            @Nullable Double mLongitude, SurveyGroup mSurveyGroup) {
        super(context, R.layout.surveyed_locale_item);
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;
        this.mSurveyGroup = mSurveyGroup;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.surveyed_locale_item, parent, false);
        } else {
            view = convertView;
        }
        TextView nameView = (TextView) view.findViewById(R.id.locale_name);
        TextView idView = (TextView) view.findViewById(R.id.locale_id);
        TextView dateView = (TextView) view.findViewById(R.id.last_modified);
        TextView distanceView = (TextView) view.findViewById(R.id.locale_distance);
        TextView statusView = (TextView) view.findViewById(R.id.status);
        ImageView statusImage = (ImageView) view.findViewById(R.id.status_img);

        final ListDataPoint surveyedLocale = getItem(position);
        Context context = parent.getContext();
        int status = surveyedLocale.getStatus();
        nameView.setText(surveyedLocale.getDisplayName());
        idView.setText(surveyedLocale.getId());

        displayDistanceText(distanceView, getDistanceText(surveyedLocale, context));
        displayDateText(dateView, surveyedLocale.getLastModified());

        int statusRes = 0;
        String statusText = null;
        switch (status) {
            case SurveyInstanceStatus.SAVED:
                statusRes = R.drawable.record_saved_icn;
                statusText = context.getString(R.string.status_saved);
                break;
            case SurveyInstanceStatus.SUBMITTED:
            case SurveyInstanceStatus.EXPORTED:
                statusRes = R.drawable.record_exported_icn;
                statusText = context.getString(R.string.status_exported);
                break;
            case SurveyInstanceStatus.SYNCED:
            case SurveyInstanceStatus.DOWNLOADED:
                statusRes = R.drawable.record_synced_icn;
                statusText = context.getString(R.string.status_synced);
                break;
            default:
                //wrong state
                break;
        }

        statusImage.setImageResource(statusRes);
        statusView.setText(statusText);

        // Alternate background
        int attr = position % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
        final int res = PlatformUtil.getResource(context, attr);
        view.setBackgroundResource(res);
        return view;
    }

    private String getDistanceText(@NonNull ListDataPoint surveyedLocale, Context context) {
        StringBuilder builder = new StringBuilder(
                context.getString(R.string.distance_label) + " ");

        if (mLatitude != null && mLongitude != null && surveyedLocale.isLocationValid()) {
            float[] results = new float[1];
            Location.distanceBetween(mLatitude, mLongitude, surveyedLocale.getLatitude(),
                    surveyedLocale.getLongitude(), results);
            final double distance = results[0];

            builder.append(GeoUtil.getDisplayLength(distance));
            return builder.toString();
        }

        return null;
    }

    private void displayDateText(TextView tv, Long time) {
        if (time != null && time > 0) {
            tv.setVisibility(View.VISIBLE);
            int labelRes = R.string.last_modified_regular;
            if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
                labelRes = R.string.last_modified_monitored;
            }
            tv.setText(tv.getContext().getString(labelRes) + " " + new PrettyTime()
                    .format(new Date(time)));
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

    void setLocales(List<ListDataPoint> surveyedLocales) {
        clear();
        for (ListDataPoint sl : surveyedLocales) {
            add(sl);
        }
        notifyDataSetChanged();
    }

    void updateLocation(double latitude, double longitude) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }
}
