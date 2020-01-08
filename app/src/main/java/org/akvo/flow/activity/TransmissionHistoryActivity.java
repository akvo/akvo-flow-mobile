/*
 *  Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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
import android.widget.ListView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.adapter.FileTransmissionArrayAdapter;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;

/**
 * Activity to show the transmission history of all files in a survey submission
 *
 * @author Christopher Fagiani
 */
public class TransmissionHistoryActivity extends BackActivity {

    @Inject
    SurveyDbDataSource databaseAdapter;

    private Long surveyInstanceId;
    private ListView transmissionsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmission_history);
        initializeInjector();
        setupToolBar();
        transmissionsList = findViewById(R.id.transmission_list);
        surveyInstanceId = getSurveyInstanceId(savedInstanceState);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    private Long getSurveyInstanceId(Bundle savedInstanceState) {
        Long surveyInstanceId;
        if (savedInstanceState != null) {
            surveyInstanceId = savedInstanceState
                    .getLong(ConstantUtil.RESPONDENT_ID_EXTRA);
        } else {
            Bundle extras = getIntent().getExtras();
            surveyInstanceId = extras != null ? extras
                    .getLong(ConstantUtil.RESPONDENT_ID_EXTRA) : null;
        }
        return surveyInstanceId;
    }

    public void onResume() {
        super.onResume();
        databaseAdapter.open();
        getTransmissionData();
    }

    private void getTransmissionData() {
        List<FileTransmission> transmissionList = databaseAdapter
                .getSurveyInstanceTransmissions(surveyInstanceId);
        displayTransmissionData(transmissionList);
    }

    private void displayTransmissionData(List<FileTransmission> transmissionList) {
        FileTransmissionArrayAdapter adapter = new FileTransmissionArrayAdapter(
                this, R.layout.transmission_history_row,
                transmissionList != null ? transmissionList
                        : new ArrayList<>());
        transmissionsList.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ConstantUtil.RESPONDENT_ID_EXTRA, surveyInstanceId);
    }

    @Override
    protected void onPause() {
        if (databaseAdapter != null) {
            databaseAdapter.close();
        }
        super.onPause();
    }
}
