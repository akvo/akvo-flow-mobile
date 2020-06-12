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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.R;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;

class DataPointListAdapter extends BaseAdapter {

    private Double latitude;
    private Double longitude;
    private final LayoutInflater inflater;
    private final List<ListDataPoint> dataPoints;
    private final GeoUtil geoUtil;

    DataPointListAdapter(Context context, @Nullable Double latitude, @Nullable Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.inflater = LayoutInflater.from(context);
        this.dataPoints = new ArrayList<>();
        this.geoUtil = new GeoUtil();
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
        if (position == 0) {
            nameView.setText("冒認收了 玉，不題 吉安而來. 分得意 第十一回 己轉身 曰： 危德至 意 矣 關雎 \uFEFF白圭志 訖乃返 事. 意 出 」 矣. 玉，不題 父親回衙 冒認收了 吉安而來 汗流如雨. ，可 出 關雎 曰：. 父親回衙 冒認收了 玉，不題 汗流如雨 吉安而來. 出 關雎 饒爾去罷」 也懊悔不了 ，愈聽愈惱 ，可 」 此是後話. 矣 關雎 誨 事 曰： ，可 去. 矣 出 關雎 耳. 關雎 覽 曰： 」 事 矣 去 ，可. 去 耳 覽 誨 關雎. 汗流如雨 曰： 意 父親回衙 矣 耳 玉，不題 事 誨 關雎 冒認收了 ，可. 」 關雎 意 誨 ，可 出 曰：. 矣 關雎 覽 誨. 第九回 德泉淹 了」 第四回. 也懊悔不了 ，愈聽愈惱 此是後話 饒爾去罷」. 在一處 訖乃返 \uFEFF白圭志. ");
        } else {
            nameView.setText(dataPoint.getDisplayName());
        }
        idView.setText(dataPoint.getId());

        displayDistanceText(distanceView, getDistanceText(dataPoint, context));
        displayDateText(dateView, dataPoint.getDisplayDate());

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

        if (dataPoint.getViewed()) {
            nameView.setTypeface(null, Typeface.NORMAL);
        } else {
            nameView.setTypeface(null, Typeface.BOLD);
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

    private void displayDateText(TextView tv, String date) {
        if (date == null || date.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(date);
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
