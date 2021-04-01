/*
 *  Copyright (C) 2016-2020 Stichting Akvo (Akvo Foundation)
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.FileProvider;

import org.akvo.flow.R;
import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.activity.TransmissionHistoryActivity;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.offlinemaps.presentation.list.OfflineAreasListActivity;
import org.akvo.flow.presentation.AppDownloadDialogFragment;
import org.akvo.flow.presentation.FullImageActivity;
import org.akvo.flow.presentation.about.AboutActivity;
import org.akvo.flow.presentation.datapoints.map.one.DataPointMapActivity;
import org.akvo.flow.presentation.entity.ViewApkData;
import org.akvo.flow.presentation.geoshape.ViewGeoShapeActivity;
import org.akvo.flow.presentation.geoshape.create.CreateGeoShapeActivity;
import org.akvo.flow.presentation.help.HelpActivity;
import org.akvo.flow.presentation.legal.LegalNoticesActivity;
import org.akvo.flow.presentation.record.RecordActivity;
import org.akvo.flow.presentation.settings.PreferenceActivity;
import org.akvo.flow.presentation.signature.SignatureActivity;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.walkthrough.presentation.OfflineMapsWalkThroughActivity;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class Navigator {

    private static final String TERMS_URL = "https://akvo.org/help/akvo-policies-and-terms-2/";
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

    public void navigateToAddUser(Context context) {
        context.startActivity(new Intent(context, AddUserActivity.class));
    }

    public void navigateToRecordActivity(Context context, String surveyedLocaleId,
            SurveyGroup mSurveyGroup) {
        // Display form list and history
        Intent intent = new Intent(context, RecordActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        extras.putString(ConstantUtil.DATA_POINT_ID_EXTRA, surveyedLocaleId);
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    //TODO: confusing, too many params, use object
    public void navigateToFormActivity(Activity activity, String dataPointId, String formId,
            long formInstanceId, boolean readOnly, SurveyGroup mSurveyGroup) {
        Intent i = new Intent(activity, FormActivity.class);
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        i.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        i.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId);
        i.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, formInstanceId);
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, readOnly);
        activity.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST);
    }

    /**
     * This is when "add datapoint" is pressed
     */
    public void navigateToFormActivity(Activity activity, SurveyGroup mSurveyGroup, String formId, User user) {
        Intent i = new Intent(activity, FormActivity.class);
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        i.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, false);
        i.putExtra(ConstantUtil.VIEW_USER_EXTRA, user);
        activity.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST);
    }

    public void navigateToFormActivity(Activity activity, SurveyGroup mSurveyGroup, String formId, User user, String dataPointId) {
        Intent i = new Intent(activity, FormActivity.class);
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        i.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA, mSurveyGroup);
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, false);
        i.putExtra(ConstantUtil.VIEW_USER_EXTRA, user);
        i.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId);
        activity.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST);
    }

    public void navigateToTakePhoto(@NonNull Activity activity, Uri uri) {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = activity.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            final List<ResolveInfo> activities = packageManager
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolvedIntentInfo : activities) {
                final String name = resolvedIntentInfo.activityInfo.packageName;
                activity.grantUriPermission(name, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            activity.startActivityForResult(intent, ConstantUtil.PHOTO_ACTIVITY_REQUEST);
        } else {
            Timber.e(new Exception("No camera on device or no app found to take pictures"));
            //TODO: notify user
        }
    }

    public void navigateToTakeVideo(@NonNull Activity activity) {
        Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        if (i.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(i, ConstantUtil.VIDEO_ACTIVITY_REQUEST);
        } else {
            Timber.e(new Exception("No app found to take videos"));
        }
    }

    public void navigateToBarcodeScanner(@NonNull AppCompatActivity activity) {
        Intent intent = new Intent(ConstantUtil.BARCODE_SCAN_INTENT);
        try {
            activity.startActivityForResult(intent, ConstantUtil.SCAN_ACTIVITY_REQUEST);
        } catch (ActivityNotFoundException ex) {
            displayScannerNotFoundDialog(activity);
        }
    }

    private void displayScannerNotFoundDialog(@NonNull final AppCompatActivity activity) {
        AppDownloadDialogFragment fragment = AppDownloadDialogFragment
                .newInstance(R.string.barcode_scanner_missing_error,
                        "http://play.google.com/store/search?q=Barcode Scanner");
        fragment.show(activity.getSupportFragmentManager(),
                AppDownloadDialogFragment.TAG);
    }

    public void navigateToPlayStore(@NonNull Activity activity, String uriString) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uriString));
        activity.startActivity(intent);
    }

    public void navigateToCaddisfly(@NonNull Activity activity, Bundle data, CharSequence title) {
        Intent intent = new Intent(ConstantUtil.CADDISFLY_ACTION);
        intent.putExtras(data);
        intent.setType(ConstantUtil.CADDISFLY_MIME);
        activity.startActivityForResult(Intent.createChooser(intent, title),
                ConstantUtil.CADDISFLY_REQUEST);
    }

    public void navigateToCreateGeoShapeActivity(@NonNull Activity activity, @Nullable Bundle data) {
        Intent i = new Intent(activity, CreateGeoShapeActivity.class);
        if (data != null) {
            i.putExtras(data);
        }
        activity.startActivityForResult(i, ConstantUtil.PLOTTING_REQUEST);
    }

    public void navigateToViewGeoShapeActivity(@NonNull Context context, @Nullable Bundle data) {
        Intent i = new Intent(context, ViewGeoShapeActivity.class);
        if (data != null) {
            i.putExtras(data);
        }
        context.startActivity(i);
    }

    public void navigateToSignatureActivity(@NonNull Activity activity, @Nullable Bundle data) {
        Intent i = new Intent(activity, SignatureActivity.class);
        if (data != null) {
            i.putExtras(data);
        }
        activity.startActivityForResult(i, ConstantUtil.SIGNATURE_REQUEST);
    }

    public void navigateToMapActivity(@NonNull Context context, String recordId) {
        context.startActivity(new Intent(context, DataPointMapActivity.class)
                .putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, recordId));
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

    public void navigateToGpsFixes(AppCompatActivity activity) {
        if (activity != null) {
            PackageManager packageManager = activity.getPackageManager();
            Intent intent = packageManager
                    .getLaunchIntentForPackage(ConstantUtil.GPS_STATUS_PACKAGE_V2);
            if (intent != null) {
                activity.startActivity(intent);
            } else {
                intent = packageManager
                        .getLaunchIntentForPackage(ConstantUtil.GPS_STATUS_PACKAGE_V1);
                if (intent != null) {
                    activity.startActivity(intent);
                } else {
                    displayGpsStatusNotFoundDialog(activity);
                }
            }
        }
    }

    private void displayGpsStatusNotFoundDialog(AppCompatActivity activity) {
        AppDownloadDialogFragment fragment = AppDownloadDialogFragment
                .newInstance(R.string.no_gps_status_message,
                        "http://play.google.com/store/apps/details?id="
                                + ConstantUtil.GPS_STATUS_PACKAGE_V2);
        fragment.show(activity.getSupportFragmentManager(),
                AppDownloadDialogFragment.TAG);
    }

    public void navigateToLargeImage(AppCompatActivity activity, String filePath) {
        if (activity != null) {
            Intent intent = new Intent(activity, FullImageActivity.class);
            intent.putExtra(ConstantUtil.IMAGE_URL_EXTRA, filePath);
            ActionBar supportActionBar = activity.getSupportActionBar();
            if (supportActionBar != null) {
                CharSequence title = supportActionBar.getTitle();
                CharSequence subtitle = supportActionBar.getSubtitle();
                intent.putExtra(ConstantUtil.FORM_TITLE_EXTRA, title);
                intent.putExtra(ConstantUtil.FORM_SUBTITLE_EXTRA, subtitle);
            }
            activity.startActivity(intent);
        }
    }

    public void navigateToVideoView(Context context, Uri fileUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public void navigateToSurveyActivity(Context context) {
        Intent intent = new Intent(context, SurveyActivity.class);
        context.startActivity(intent);
    }

    /**
     * Install the newest version of the app. This method will be called
     * either after the file download is completed, or upon the app being started,
     * if the newest version is found in the filesystem.
     *
     * @param context  Context
     * @param filename Absolute path to the newer APK
     */
    public void installAppUpdate(Context context, String filename) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri fileUri;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider
                    .getUriForFile(context, ConstantUtil.FILE_PROVIDER_AUTHORITY,
                            new File(filename));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            fileUri = Uri.fromFile(new File(filename));
        }
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public void navigateToWalkThrough(Context context) {
        TaskStackBuilder.create(context)
                .addParentStack(OfflineMapsWalkThroughActivity.class)
                .addNextIntent(new Intent(context, OfflineMapsWalkThroughActivity.class))
                .startActivities();
    }

    public void navigateToGetPhoto(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, ConstantUtil.GET_PHOTO_ACTIVITY_REQUEST);
        }
    }

    public void navigateToGetVideo(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, ConstantUtil.GET_VIDEO_ACTIVITY_REQUEST);
        }
    }

    public void navigateToAppSystemSettings(@Nullable Context context) {
        if (context != null) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void navigateToOfflineAreasList(@Nullable Context context) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineAreasListActivity.class);
            context.startActivity(intent);
        }
    }
}
