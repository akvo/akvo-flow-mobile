package org.akvo.flow.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.IOException;
import org.akvo.flow.api.service.ApkApiService;
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.domain.apkupdate.ApkUpdateMapper;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class ApkUpdateHelper {

    private final ApkApiService apkApiService = new ApkApiService();
    private final ApkUpdateMapper apkUpdateMapper = new ApkUpdateMapper();
    private final Navigator navigator = new Navigator();

    public ApkUpdateHelper() {
    }

    void checkUpdate(@NonNull Context context) throws IOException, JSONException {
        JSONObject json = apkApiService.getApkDataObject(context);
        ApkData data = apkUpdateMapper.transform(json);
        if (shouldAppBeUpdated(data, context)) {
            // There is a newer version. Fire the 'Download and Install' Activity.
            navigator.navigateToAppUpdate(context, data);
        }
    }

    private boolean shouldAppBeUpdated(@Nullable ApkData data, @NonNull Context context) {
        if (data == null) {
            return false;
        }
        String version = data.getVersion();
        return StringUtil.isValid(version)
            && PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(context), version)
            && StringUtil.isValid(data.getFileUrl());
    }
}
