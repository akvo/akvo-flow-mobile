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
