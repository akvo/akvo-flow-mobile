package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.PlatformUtil;

public class SurveyListAdapter extends CursorAdapter {
    final int mTextColor;

    public SurveyListAdapter(Context context) {
        super(context, null, 0);
        mTextColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.survey_group_list_item, null);
        bindView(view, context, cursor);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final SurveyGroup surveyGroup = SurveyDbAdapter.getSurveyGroup(cursor);

        TextView text1 = (TextView)view.findViewById(R.id.text1);
        text1.setText(surveyGroup.getName());
        text1.setTextColor(context.getResources().getColorStateList(mTextColor));

        // Alternate background
        int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
        final int res= PlatformUtil.getResource(context, attr);
        view.setBackgroundResource(res);

        view.setTag(surveyGroup);
    }

}
