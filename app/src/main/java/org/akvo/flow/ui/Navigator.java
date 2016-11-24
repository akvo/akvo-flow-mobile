package org.akvo.flow.ui;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.util.StringUtil;

public class Navigator {

    public Navigator() {
    }

    public void navigateToAppUpdate(@NonNull Context context, @NonNull ApkData data) {
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
}