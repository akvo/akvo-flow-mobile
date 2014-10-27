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
import org.akvo.flow.dao.SurveyDbAdapter.Tables;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyColumns;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
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
        final int status = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.STATUS));

        // This default values should NEVER be displayed
        String statusText = "";
        int icon = R.drawable.red_cross;
        boolean finished = false;
        long displayDate = 0L;
        switch (status) {
            case SurveyInstanceStatus.SAVED:
                statusText = context.getString(R.string.status_saved) + ": ";
                icon = R.drawable.form_saved_icn;
                displayDate = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SAVED_DATE));
                break;
            case SurveyInstanceStatus.SUBMITTED:
                statusText = context.getString(R.string.status_submitted) + ": ";
                displayDate = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE));
                icon = R.drawable.exported_icn;
                finished = true;
                break;
            case SurveyInstanceStatus.EXPORTED:
                statusText = context.getString(R.string.status_exported) + ": ";
                displayDate = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.EXPORTED_DATE));
                icon = R.drawable.exported_icn;
                finished = true;
                break;
            case SurveyInstanceStatus.SYNCED:
            case SurveyInstanceStatus.DOWNLOADED:
                statusText = context.getString(R.string.status_synced) + ": ";
                displayDate = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SYNC_DATE));
                icon = R.drawable.checkmark;
                finished = true;
                break;
        }

        TextView userView = (TextView) view.findViewById(R.id.text2);
        TextView dateView = (TextView) view.findViewById(R.id.text3);

        String username = cursor.getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTER));
        if (TextUtils.isEmpty(username)) {
            userView.setVisibility(View.GONE);
        } else {
            userView.setVisibility(View.VISIBLE);
            userView.setText(username);
        }

        // Format the date string
        Date date = new Date(displayDate);
        dateView.setText(statusText
                + DateFormat.getLongDateFormat(context).format(date) + " "
                + DateFormat.getTimeFormat(context).format(date));
        TextView headingView = (TextView) view.findViewById(R.id.text1);
        headingView.setText(cursor.getString(cursor.getColumnIndex(SurveyColumns.NAME)));
        view.setTag(SURVEY_ID_KEY, cursor.getLong(cursor
                .getColumnIndex(SurveyInstanceColumns.SURVEY_ID)));
        view.setTag(RESP_ID_KEY, cursor.getLong(cursor
                .getColumnIndex(SurveyInstanceColumns._ID)));
        view.setTag(RECORD_KEY, cursor.getString(cursor
                .getColumnIndex(SurveyInstanceColumns.RECORD_ID)));
        view.setTag(FINISHED_KEY, finished);
        ImageView stsIcon = (ImageView) view.findViewById(R.id.xmitstsicon);
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
