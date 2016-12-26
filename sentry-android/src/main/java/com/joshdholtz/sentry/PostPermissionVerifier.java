package com.joshdholtz.sentry;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class PostPermissionVerifier extends AbstractPermissionVerifier {

    public PostPermissionVerifier() {
    }

    /**
     * Decides if the stacktrace should be sent to the server
     *
     * @return
     * @param context
     */
    @Override
    public boolean shouldAttemptPost(Context context) {
        //Is the permission set in manifest?
        PackageManager pm = context.getPackageManager();
        int hasPerm = pm.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE,
                context.getPackageName());
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        //is there a connection?
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}