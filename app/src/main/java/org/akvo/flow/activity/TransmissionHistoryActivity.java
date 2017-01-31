/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Window;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.ui.adapter.FileTransmissionArrayAdapter;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to show the transmission history of all files in a survey submission
 * 
 * @author Christopher Fagiani
 */
public class TransmissionHistoryActivity extends ListActivity {
    private SurveyDbAdapter databaseAdapter;
    private Long respondentId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (savedInstanceState != null) {
            respondentId = savedInstanceState
                    .getLong(ConstantUtil.RESPONDENT_ID_KEY);
        } else {
            Bundle extras = getIntent().getExtras();
            respondentId = extras != null ? extras
                    .getLong(ConstantUtil.RESPONDENT_ID_KEY) : null;
        }
        setContentView(R.layout.transmissionhistory);
        databaseAdapter = new SurveyDbAdapter(this);

    }

    public void onResume() {
        super.onResume();
        databaseAdapter.open();
        getData();
    }

    private void getData() {
        List<FileTransmission> transmissionList = databaseAdapter.getFileTransmissions(respondentId);
        FileTransmissionArrayAdapter adapter = new FileTransmissionArrayAdapter(
                this, R.layout.transmissionrow,
                transmissionList != null ? transmissionList
                        : new ArrayList<FileTransmission>());
        setListAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putLong(ConstantUtil.RESPONDENT_ID_KEY, respondentId);
        }
    }

    protected void onPause() {
        if (databaseAdapter != null) {
            databaseAdapter.close();
        }
        super.onPause();
    }

}
