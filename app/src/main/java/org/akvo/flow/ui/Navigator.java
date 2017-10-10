/*
 *  Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import org.akvo.flow.R;
import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.GeoshapeActivity;
import org.akvo.flow.activity.MapActivity;
import org.akvo.flow.activity.RecordActivity;
import org.akvo.flow.activity.TransmissionHistoryActivity;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.presentation.AboutActivity;
import org.akvo.flow.presentation.help.HelpActivity;
import org.akvo.flow.presentation.legal.LegalNoticesActivity;
import org.akvo.flow.presentation.settings.PreferenceActivity;
import org.akvo.flow.presentation.signature.SignatureActivity;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StringUtil;

import javax.inject.Inject;

import static org.akvo.flow.util.ConstantUtil.REQUEST_ADD_USER;

public class Navigator {

    private static final String TERMS_URL = "http://akvo.org/help/akvo-policies-and-terms-2/akvo-flow-terms-of-use/";
    private static final String RELEASE_NOTES_URL = "https://github.com/akvo/akvo-flow-mobile/releases";

    //TODO: inject activity
    @Inject
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
        extras.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        extras.putString(ConstantUtil.RECORD_ID_EXTRA, surveyedLocaleId);
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    //TODO: confusing, too many params, use object
    public void navigateToFormActivity(Context context, String surveyedLocaleId, String formId,
            long formInstanceId, boolean readOnly, SurveyGroup mSurveyGroup) {
        Intent i = new Intent(context, FormActivity.class);
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        i.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA, surveyedLocaleId);
        i.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, formInstanceId);
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, readOnly);
        context.startActivity(i);
    }

    public void navigateToTakePhoto(@NonNull Activity activity, Uri uri) {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(i, ConstantUtil.PHOTO_ACTIVITY_REQUEST);
    }

    public void navigateToTakeVideo(@NonNull Activity activity, Uri uri) {
        Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(i, ConstantUtil.VIDEO_ACTIVITY_REQUEST);
    }

    public void navigateToBarcodeScanner(@NonNull Activity activity) {
        Intent intent = new Intent(ConstantUtil.BARCODE_SCAN_INTENT);
        try {
            activity.startActivityForResult(intent, ConstantUtil.SCAN_ACTIVITY_REQUEST);
        } catch (ActivityNotFoundException ex) {
            displayScannerNotFoundDialog(activity, R.string.barcode_scanner_missing_error);
        }
    }

    private void displayScannerNotFoundDialog(@NonNull final Activity activity, @StringRes int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.install,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("http://play.google.com/store/search?q=Barcode Scanner"));
                        activity.startActivity(intent);
                    }
                });
        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    public void navigateToExternalSource(@NonNull Activity activity, Bundle data,
            CharSequence title) {
        Intent intent = new Intent(ConstantUtil.EXTERNAL_SOURCE_ACTION);
        intent.putExtras(data);
        intent.setType(ConstantUtil.CADDISFLY_MIME);
        activity.startActivityForResult(Intent.createChooser(intent, title),
                ConstantUtil.EXTERNAL_SOURCE_REQUEST);
    }

    public void navigateToCaddisfly(@NonNull Activity activity, Bundle data, CharSequence title) {
        Intent intent = new Intent(ConstantUtil.CADDISFLY_ACTION);
        intent.putExtras(data);
        intent.setType(ConstantUtil.CADDISFLY_MIME);
        activity.startActivityForResult(Intent.createChooser(intent, title),
                ConstantUtil.CADDISFLY_REQUEST);
    }

    public void navigateToGeoShapeActivity(@NonNull Activity activity, @Nullable Bundle data) {
        Intent i = new Intent(activity, GeoshapeActivity.class);
        if (data != null) {
            i.putExtras(data);
        }
        activity.startActivityForResult(i, ConstantUtil.PLOTTING_REQUEST);
    }

    public void navigateToSignatureActivity(@NonNull Activity activity, @Nullable Bundle data) {
        Intent i = new Intent(activity, SignatureActivity.class);
        if (data != null) {
            i.putExtras(data);
        }
        activity.startActivityForResult(i, ConstantUtil.SIGNATURE_REQUEST);
    }

    public void navigateToMapActivity(@NonNull Context context, String recordId) {
        context.startActivity(new Intent(context, MapActivity.class)
                .putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA, recordId));
    }

    public void navigateToTransmissionActivity(Context context, long surveyInstanceId) {
        context.startActivity(new Intent(context, TransmissionHistoryActivity.class)
                .putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, surveyInstanceId));
    }

    public void navigateToLocationSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent);
        } else {
            navigateToSettings(context);
        }
    }

    public void navigateToStorageSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent);
        } else {
            intent = new Intent(Settings.ACTION_MEMORY_CARD_SETTINGS);
            packageManager = context.getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent);
            } else {
                navigateToSettings(context);
            }
        }
    }

    /**
     * Fallback as Settings.ACTION_LOCATION_SOURCE_SETTINGS may not be available on some devices
     */
    private void navigateToSettings(@NonNull Context context) {
        context.startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    public void navigateToAbout(@NonNull Context context) {
        context.startActivity(new Intent(context, AboutActivity.class));
    }

    public void navigateToTerms(@NonNull Context context) {
        openUrl(context, TERMS_URL);
    }

    public void openUrl(@NonNull Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }

    public void navigateToReleaseNotes(@NonNull Context context) {
        openUrl(context, RELEASE_NOTES_URL);
    }

    public void navigateToLegalInfo(@NonNull Context context) {
        context.startActivity(new Intent(context, LegalNoticesActivity.class));
    }

    public void navigateToAppSettings(@NonNull Context context) {
        context.startActivity(new Intent(context, PreferenceActivity.class));
    }

    public void navigateToHelp(@NonNull Context context) {
        context.startActivity(new Intent(context, HelpActivity.class));
    }
}
