/*
 *  Copyright (C) 2015-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.cursoradapter.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

import java.util.Date;

public class ResponseListAdapter extends CursorAdapter {

    private static final String DEFAULT_USERNAME = "IMPORTER";

    private final int[] backgrounds = new int[2];

    public ResponseListAdapter(Context activityContext) {
        super(activityContext.getApplicationContext(), null, false);
        backgrounds[0] = PlatformUtil.getResource(activityContext, R.attr.listitem_bg1);
        backgrounds[1] = PlatformUtil.getResource(activityContext, R.attr.listitem_bg2);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final int status = cursor.getInt(SurveyDbAdapter.FormInstanceQuery.STATUS);

        int icon = 0;
        // Default to 'Submitted' status values
        String statusText = context.getString(R.string.status_submitted) + ": ";
        long displayDate = cursor.getLong(SurveyDbAdapter.FormInstanceQuery.SUBMITTED_DATE);

        switch (status) {
            case SurveyInstanceStatus.SAVED:
            case SurveyInstanceStatus.SUBMIT_REQUESTED:
                icon = R.drawable.form_saved_icn;
                statusText = context.getString(R.string.status_saved) + ": ";
                displayDate = cursor.getLong(SurveyDbAdapter.FormInstanceQuery.SAVED_DATE);
                break;
            case SurveyInstanceStatus.SUBMITTED:
                icon = R.drawable.submitted_icn;
                break;
            case SurveyInstanceStatus.UPLOADED:
            case SurveyInstanceStatus.DOWNLOADED:
                icon = R.drawable.checkmark;
                break;
            default:
                break;

        }

        TextView userView = (TextView) view.findViewById(R.id.username);
        TextView statusView = (TextView) view.findViewById(R.id.status);

        String username = cursor.getString(SurveyDbAdapter.FormInstanceQuery.SUBMITTER);
        if (TextUtils.isEmpty(username)) {
            username = DEFAULT_USERNAME;
        }
        userView.setText(username);

        // Format the date string
        Date date = new Date(displayDate);
        statusView.setText(statusText
                + DateFormat.getLongDateFormat(context).format(date) + " "
                + DateFormat.getTimeFormat(context).format(date));
        TextView headingView = (TextView) view.findViewById(R.id.form_name);
        headingView.setText(cursor.getString(SurveyDbAdapter.FormInstanceQuery.NAME));
        view.setTag(ConstantUtil.SURVEY_ID_TAG_KEY, cursor.getLong(
                SurveyDbAdapter.FormInstanceQuery.SURVEY_ID));
        view.setTag(ConstantUtil.RESPONDENT_ID_TAG_KEY, cursor.getLong(
                SurveyDbAdapter.FormInstanceQuery._ID));
        view.setTag(ConstantUtil.READ_ONLY_TAG_KEY, status != SurveyInstanceStatus.SAVED);
        ImageView stsIcon = (ImageView) view.findViewById(R.id.status_img);
        stsIcon.setImageResource(icon);

        // Alternate background
        int backgroundIndex = cursor.getPosition() % 2 == 0 ? 0 : 1;
        view.setBackgroundResource(backgrounds[backgroundIndex]);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Context activityContext = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) activityContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.submittedrow, parent, false);
    }
}
