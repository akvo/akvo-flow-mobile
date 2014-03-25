/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Window;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.ui.adapter.FileTransmissionArrayAdapter;

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

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onPause() {
        if (databaseAdapter != null) {
            databaseAdapter.close();
        }
        super.onPause();
    }

}
