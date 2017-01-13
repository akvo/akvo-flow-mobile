/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.data.database.SurveyDbAdapter.FormInstanceQuery;
import org.akvo.flow.util.PlatformUtil;

import java.util.Date;

public class ResponseListAdapter extends CursorAdapter {
    public static final int SURVEY_ID_KEY = R.integer.surveyidkey;
    public static final int RESP_ID_KEY = R.integer.respidkey;
    public static final int FINISHED_KEY = R.integer.finishedkey;
    public static final int RECORD_KEY = R.integer.recordkey;

    public ResponseListAdapter(Context context) {
        super(context, null, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final int status = cursor.getInt(FormInstanceQuery.STATUS);

        // Default to 'Submitted' status values
        int icon = 0;
        String statusText = context.getString(R.string.status_submitted) + ": ";
        long displayDate = cursor.getLong(FormInstanceQuery.SUBMITTED_DATE);

        switch (status) {
            case SurveyInstanceStatus.SAVED:
                icon = R.drawable.form_saved_icn;
                statusText = context.getString(R.string.status_saved) + ": ";
                displayDate = cursor.getLong(FormInstanceQuery.SAVED_DATE);
                break;
            case SurveyInstanceStatus.SUBMITTED:
            case SurveyInstanceStatus.EXPORTED:
                icon = R.drawable.exported_icn;
                break;
            case SurveyInstanceStatus.SYNCED:
            case SurveyInstanceStatus.DOWNLOADED:
                icon = R.drawable.checkmark;
                break;
        }

        TextView userView = (TextView) view.findViewById(R.id.username);
        TextView statusView = (TextView) view.findViewById(R.id.status);

        String username = cursor.getString(FormInstanceQuery.SUBMITTER);
        if (TextUtils.isEmpty(username)) {
            userView.setVisibility(View.GONE);
        } else {
            userView.setVisibility(View.VISIBLE);
            userView.setText(username);
        }

        // Format the date string
        Date date = new Date(displayDate);
        statusView.setText(statusText
                + DateFormat.getLongDateFormat(context).format(date) + " "
                + DateFormat.getTimeFormat(context).format(date));
        TextView headingView = (TextView) view.findViewById(R.id.form_name);
        headingView.setText(cursor.getString(FormInstanceQuery.NAME));
        view.setTag(SURVEY_ID_KEY, cursor.getLong(FormInstanceQuery.SURVEY_ID));
        view.setTag(RESP_ID_KEY, cursor.getLong(FormInstanceQuery._ID));
        view.setTag(RECORD_KEY, cursor.getString(FormInstanceQuery.RECORD_ID));
        view.setTag(FINISHED_KEY, status != SurveyInstanceStatus.SAVED);
        ImageView stsIcon = (ImageView) view.findViewById(R.id.status_img);
        stsIcon.setImageResource(icon);

        // Alternate background
        int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1
                : R.attr.listitem_bg2;
        final int res= PlatformUtil.getResource(context, attr);
        view.setBackgroundResource(res);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.submittedrow, parent, false);
    }

}
