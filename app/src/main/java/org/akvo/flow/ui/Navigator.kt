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
package org.akvo.flow.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import org.akvo.flow.BuildConfig
import org.akvo.flow.R
import org.akvo.flow.activity.AddUserActivity
import org.akvo.flow.activity.AppUpdateActivity
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.SurveyActivity
import org.akvo.flow.activity.TransmissionHistoryActivity
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.User
import org.akvo.flow.offlinemaps.presentation.list.OfflineAreasListActivity
import org.akvo.flow.presentation.AppDownloadDialogFragment
import org.akvo.flow.presentation.FullImageActivity
import org.akvo.flow.presentation.about.AboutActivity
import org.akvo.flow.presentation.datapoints.map.one.DataPointMapActivity
import org.akvo.flow.presentation.entity.ViewApkData
import org.akvo.flow.presentation.form.view.FormViewActivity
import org.akvo.flow.presentation.geoshape.ViewGeoShapeActivity
import org.akvo.flow.presentation.geoshape.create.CreateGeoShapeActivity
import org.akvo.flow.presentation.help.HelpActivity
import org.akvo.flow.presentation.legal.LegalNoticesActivity
import org.akvo.flow.presentation.record.RecordActivity
import org.akvo.flow.presentation.settings.PreferenceActivity
import org.akvo.flow.presentation.signature.SignatureActivity
import org.akvo.flow.util.ConstantUtil
import org.akvo.flow.util.StringUtil
import org.akvo.flow.walkthrough.presentation.OfflineMapsWalkThroughActivity
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class Navigator @Inject constructor() {
    fun navigateToAppUpdate(context: Context, data: ViewApkData) {
        val i = Intent(context, AppUpdateActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.putExtra(AppUpdateActivity.EXTRA_URL, data.fileUrl)
        i.putExtra(AppUpdateActivity.EXTRA_VERSION, data.version)
        val md5Checksum = data.md5Checksum
        if (StringUtil.isValid(md5Checksum)) {
            i.putExtra(AppUpdateActivity.EXTRA_CHECKSUM, md5Checksum)
        }
        context.startActivity(i)
    }

    fun navigateToAddUser(context: Context) {
        context.startActivity(Intent(context, AddUserActivity::class.java))
    }

    fun navigateToRecordActivity(
        context: Context, surveyedLocaleId: String?,
        mSurveyGroup: SurveyGroup?
    ) {
        // Display form list and history
        val intent = Intent(context, RecordActivity::class.java)
        val extras = Bundle()
        extras.putSerializable(ConstantUtil.SURVEY_EXTRA, mSurveyGroup)
        extras.putString(ConstantUtil.DATA_POINT_ID_EXTRA, surveyedLocaleId)
        intent.putExtras(extras)
        context.startActivity(intent)
    }

    //TODO: confusing, too many params, use object
    fun navigateToFormActivity(
        activity: FragmentActivity?, dataPointId: String?, formId: String?,
        formInstanceId: Long, readOnly: Boolean, survey: SurveyGroup?
    ) {
        if (readOnly && isInternalInstance()) {
            val i = Intent(activity, FormViewActivity::class.java)
            i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId)
            i.putExtra(ConstantUtil.SURVEY_EXTRA, survey)
            i.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId)
            i.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, formInstanceId)
            activity?.startActivity(i)
        } else {
            val i = Intent(activity, FormActivity::class.java)
            i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId)
            i.putExtra(ConstantUtil.SURVEY_EXTRA, survey)
            i.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId)
            i.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, formInstanceId)
            i.putExtra(ConstantUtil.READ_ONLY_EXTRA, readOnly)
            activity?.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST)
        }
    }

    private fun isInternalInstance(): Boolean {
        val debugInstances = listOf(
            "akvoflow-uat1",
            "akvoflow-uat2",
            "akvoflow-dev1",
            "akvoflow-dev2",
            "akvoflow-dev3",
            "akvoflow-45",
            "akvoflow-60",
            "akvoflow-62",
            "akvoflow-106",
            "akvoflow-hub2",
            "akvoflow-internal2",
            "akvoflow-163",
            "akvoflow-168",
            "akvoflow-185",
            "akvoflow-197",
            "akvoflow-206",
            "akvoflow-213",
        )
        return debugInstances.contains(BuildConfig.AWS_BUCKET)
    }

    /**
     * This is when "add datapoint" is pressed
     */
    fun navigateToFormActivity(
        activity: Activity,
        mSurveyGroup: SurveyGroup?,
        formId: String?,
        user: User?
    ) {
        val i = Intent(activity, FormActivity::class.java)
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId)
        i.putExtra(ConstantUtil.SURVEY_EXTRA, mSurveyGroup)
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, false)
        i.putExtra(ConstantUtil.VIEW_USER_EXTRA, user)
        activity.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST)
    }

    fun navigateToFormActivity(
        activity: Activity,
        mSurveyGroup: SurveyGroup?,
        formId: String?,
        user: User?,
        dataPointId: String?
    ) {
        val i = Intent(activity, FormActivity::class.java)
        i.putExtra(ConstantUtil.FORM_ID_EXTRA, formId)
        i.putExtra(ConstantUtil.SURVEY_EXTRA, mSurveyGroup)
        i.putExtra(ConstantUtil.READ_ONLY_EXTRA, false)
        i.putExtra(ConstantUtil.VIEW_USER_EXTRA, user)
        i.putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, dataPointId)
        activity.startActivityForResult(i, ConstantUtil.FORM_FILLING_REQUEST)
    }

    fun navigateToTakePhoto(activity: Activity, uri: Uri?) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = activity.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            val activities = packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolvedIntentInfo in activities) {
                val name = resolvedIntentInfo.activityInfo.packageName
                activity.grantUriPermission(name, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            activity.startActivityForResult(intent, ConstantUtil.PHOTO_ACTIVITY_REQUEST)
        } else {
            Timber.e(Exception("No camera on device or no app found to take pictures"))
            //TODO: notify user
        }
    }

    fun navigateToTakeVideo(activity: Activity) {
        val i = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (i.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(i, ConstantUtil.VIDEO_ACTIVITY_REQUEST)
        } else {
            Timber.e(Exception("No app found to take videos"))
        }
    }

    fun navigateToBarcodeScanner(activity: AppCompatActivity) {
        val intent = Intent(ConstantUtil.BARCODE_SCAN_INTENT)
        try {
            activity.startActivityForResult(intent, ConstantUtil.SCAN_ACTIVITY_REQUEST)
        } catch (ex: ActivityNotFoundException) {
            displayScannerNotFoundDialog(activity)
        }
    }

    private fun displayScannerNotFoundDialog(activity: AppCompatActivity) {
        val fragment = AppDownloadDialogFragment
            .newInstance(R.string.barcode_scanner_missing_error,
                "http://play.google.com/store/search?q=Barcode Scanner")
        fragment.show(activity.supportFragmentManager,
            AppDownloadDialogFragment.TAG)
    }

    fun navigateToPlayStore(activity: Activity, uriString: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(uriString)
        activity.startActivity(intent)
    }

    fun navigateToCaddisfly(activity: Activity, data: Bundle?, title: CharSequence?) {
        val intent = Intent(ConstantUtil.CADDISFLY_ACTION)
        intent.putExtras(data!!)
        intent.type = ConstantUtil.CADDISFLY_MIME
        activity.startActivityForResult(Intent.createChooser(intent, title),
            ConstantUtil.CADDISFLY_REQUEST)
    }

    fun navigateToCreateGeoShapeActivity(activity: Activity, data: Bundle?) {
        val i = Intent(activity, CreateGeoShapeActivity::class.java)
        if (data != null) {
            i.putExtras(data)
        }
        activity.startActivityForResult(i, ConstantUtil.PLOTTING_REQUEST)
    }

    fun navigateToViewGeoShapeActivity(context: Context, data: Bundle?) {
        val i = Intent(context, ViewGeoShapeActivity::class.java)
        if (data != null) {
            i.putExtras(data)
        }
        context.startActivity(i)
    }

    fun navigateToSignatureActivity(activity: Activity, data: Bundle?) {
        val i = Intent(activity, SignatureActivity::class.java)
        if (data != null) {
            i.putExtras(data)
        }
        activity.startActivityForResult(i, ConstantUtil.SIGNATURE_REQUEST)
    }

    fun navigateToMapActivity(context: Context, recordId: String?) {
        context.startActivity(Intent(context, DataPointMapActivity::class.java)
            .putExtra(ConstantUtil.DATA_POINT_ID_EXTRA, recordId))
    }

    fun navigateToTransmissionActivity(context: FragmentActivity?, surveyInstanceId: Long) {
        context?.startActivity(Intent(context, TransmissionHistoryActivity::class.java)
            .putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, surveyInstanceId))
    }

    fun navigateToLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        } else {
            navigateToSettings(context)
        }
    }

    fun navigateToStorageSettings(context: Context) {
        var intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        } else {
            intent = Intent(Settings.ACTION_MEMORY_CARD_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
            } else {
                navigateToSettings(context)
            }
        }
    }

    /**
     * Fallback as Settings.ACTION_LOCATION_SOURCE_SETTINGS may not be available on some devices
     */
    private fun navigateToSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    fun navigateToAbout(context: Context) {
        context.startActivity(Intent(context, AboutActivity::class.java))
    }

    fun navigateToTerms(context: Context) {
        openUrl(context, TERMS_URL)
    }

    fun openUrl(context: Context, url: String?) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }

    fun navigateToReleaseNotes(context: Context) {
        openUrl(context, RELEASE_NOTES_URL)
    }

    fun navigateToLegalInfo(context: Context) {
        context.startActivity(Intent(context, LegalNoticesActivity::class.java))
    }

    fun navigateToAppSettings(context: Context) {
        context.startActivity(Intent(context, PreferenceActivity::class.java))
    }

    fun navigateToHelp(context: Context) {
        context.startActivity(Intent(context, HelpActivity::class.java))
    }

    fun navigateToGpsFixes(activity: AppCompatActivity?) {
        if (activity != null) {
            val packageManager = activity.packageManager
            var intent = packageManager
                .getLaunchIntentForPackage(ConstantUtil.GPS_STATUS_PACKAGE_V2)
            if (intent != null) {
                activity.startActivity(intent)
            } else {
                intent = packageManager
                    .getLaunchIntentForPackage(ConstantUtil.GPS_STATUS_PACKAGE_V1)
                if (intent != null) {
                    activity.startActivity(intent)
                } else {
                    displayGpsStatusNotFoundDialog(activity)
                }
            }
        }
    }

    private fun displayGpsStatusNotFoundDialog(activity: AppCompatActivity) {
        val fragment = AppDownloadDialogFragment
            .newInstance(R.string.no_gps_status_message,
                "http://play.google.com/store/apps/details?id="
                        + ConstantUtil.GPS_STATUS_PACKAGE_V2)
        fragment.show(activity.supportFragmentManager,
            AppDownloadDialogFragment.TAG)
    }

    fun navigateToLargeImage(activity: AppCompatActivity?, filePath: String?) {
        if (activity != null) {
            val intent = Intent(activity, FullImageActivity::class.java)
            intent.putExtra(ConstantUtil.IMAGE_URL_EXTRA, filePath)
            val supportActionBar = activity.supportActionBar
            if (supportActionBar != null) {
                val title = supportActionBar.title
                val subtitle = supportActionBar.subtitle
                intent.putExtra(ConstantUtil.FORM_TITLE_EXTRA, title)
                intent.putExtra(ConstantUtil.FORM_SUBTITLE_EXTRA, subtitle)
            }
            activity.startActivity(intent)
        }
    }

    fun navigateToVideoView(context: Context, fileUri: Uri?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "video/mp4")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun navigateToSurveyActivity(context: Context) {
        val intent = Intent(context, SurveyActivity::class.java)
        context.startActivity(intent)
    }

    /**
     * Install the newest version of the app. This method will be called
     * either after the file download is completed, or upon the app being started,
     * if the newest version is found in the filesystem.
     *
     * @param context  Context
     * @param filename Absolute path to the newer APK
     */
    fun installAppUpdate(context: Context, filename: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fileUri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider
                .getUriForFile(context, ConstantUtil.FILE_PROVIDER_AUTHORITY,
                    File(filename))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            fileUri = Uri.fromFile(File(filename))
        }
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

    fun navigateToWalkThrough(context: Context?) {
        TaskStackBuilder.create(context!!)
            .addParentStack(OfflineMapsWalkThroughActivity::class.java)
            .addNextIntent(Intent(context, OfflineMapsWalkThroughActivity::class.java))
            .startActivities()
    }

    fun navigateToGetPhoto(activity: AppCompatActivity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, ConstantUtil.GET_PHOTO_ACTIVITY_REQUEST)
        }
    }

    fun navigateToGetVideo(activity: AppCompatActivity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, ConstantUtil.GET_VIDEO_ACTIVITY_REQUEST)
        }
    }

    fun navigateToAppSystemSettings(context: Context?) {
        if (context != null) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun navigateToOfflineAreasList(context: Context?) {
        if (context != null) {
            val intent = Intent(context, OfflineAreasListActivity::class.java)
            context.startActivity(intent)
        }
    }

    companion object {
        private const val TERMS_URL = "https://akvo.org/help/akvo-policies-and-terms-2/"
        private const val RELEASE_NOTES_URL = "https://github.com/akvo/akvo-flow-mobile/releases"
    }
}