/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;

public class FormDeletedReceiver extends BroadcastReceiver {
    public static final String FORM_ID = "formId";

    public void onReceive(Context context, Intent intent) {
        String formId = intent.getStringExtra(FORM_ID);

        SurveyDbAdapter db = new SurveyDbAdapter(context).open();
        db.deleteSurvey(formId, false);
        db.close();

        Toast.makeText(context, "Form " + formId + " has been removed", Toast.LENGTH_SHORT)
                .show();
        SurveyDownloadService.sendBroadcastNotification(context);
    }
}
