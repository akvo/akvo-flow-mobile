package org.akvo.flow.api.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.IOException;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.StatusUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class ApkApiService {

    private static final String APK_VERSION_SERVICE_PATH =
        "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp";

    @Nullable
    public JSONObject getApkDataObject(@NonNull Context context) throws IOException, JSONException {
        final String url = StatusUtil.getServerBase(context) + APK_VERSION_SERVICE_PATH;
        String response = HttpUtil.httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            return new JSONObject(response);
        }
        return null;
    }
}
