package org.akvo.flow.domain.apkupdate;

import android.support.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class ApkUpdateMapper {

    @Nullable
    public ApkData transform(@Nullable JSONObject json) throws JSONException {
        if (json == null) {
            return null;
        }
        String latestVersion = json.getString("version");
        String apkUrl = json.getString("fileName");
        String md5Checksum = json.optString("md5Checksum", null);
        return new ApkData(latestVersion, apkUrl, md5Checksum);
    }
}
