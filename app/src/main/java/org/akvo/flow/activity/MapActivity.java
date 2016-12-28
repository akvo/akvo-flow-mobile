package org.akvo.flow.activity;

import android.os.Bundle;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.fragment.MapFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;

public class MapActivity extends BackActivity implements RecordListListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        String datapointId = getIntent().getStringExtra(ConstantUtil.SURVEYED_LOCALE_ID);

        SurveyDbAdapter db = new SurveyDbAdapter(this).open();
        SurveyedLocale datapoint = db.getSurveyedLocale(datapointId);
        db.close();

        if (datapoint != null) {
            setTitle(datapoint.getDisplayName(this));
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, MapFragment.newInstance(null, datapointId))
                .commit();
    }

    @Override
    public void onRecordSelected(String surveyedLocaleId) {
    }

}
