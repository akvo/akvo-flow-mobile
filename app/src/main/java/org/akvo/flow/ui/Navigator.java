/*
 *  Copyright (C) 2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.RecordActivity;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StringUtil;

import static org.akvo.flow.util.ConstantUtil.REQUEST_ADD_USER;

public class Navigator {

    public Navigator() {
    }

    public void navigateToAppUpdate(@NonNull Context context, @NonNull ViewApkData data) {
        Intent i = new Intent(context, AppUpdateActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(AppUpdateActivity.EXTRA_URL, data.getFileUrl());
        i.putExtra(AppUpdateActivity.EXTRA_VERSION, data.getVersion());
        String md5Checksum = data.getMd5Checksum();
        if (StringUtil.isValid(md5Checksum)) {
            i.putExtra(AppUpdateActivity.EXTRA_CHECKSUM, md5Checksum);
        }
        context.startActivity(i);
    }

    public void navigateToAddUser(Activity activity) {
        activity.startActivityForResult(new Intent(activity, AddUserActivity.class),
                REQUEST_ADD_USER);
    }

    public void navigateToRecordActivity(Context context, String surveyedLocaleId,
            SurveyGroup mSurveyGroup) {
        // Display form list and history
        Intent intent = new Intent(context, RecordActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(RecordActivity.EXTRA_SURVEY_GROUP, mSurveyGroup);
        extras.putString(RecordActivity.EXTRA_RECORD_ID, surveyedLocaleId);
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    public void navigateToFormActivity(Context context, String surveyedLocaleId, User user,
            String formId,
            long formInstanceId, boolean readOnly, SurveyGroup mSurveyGroup) {
        Intent i = new Intent(context, FormActivity.class);
        i.putExtra(ConstantUtil.USER_ID_KEY, user.getId());
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, formId);
        i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
        i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, surveyedLocaleId);
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, formInstanceId);
        i.putExtra(ConstantUtil.READONLY_KEY, readOnly);
        context.startActivity(i);
    }
}