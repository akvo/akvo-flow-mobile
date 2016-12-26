package com.joshdholtz.sentry;

import android.content.Context;

public abstract class AbstractPermissionVerifier {

    public abstract boolean shouldAttemptPost(Context context);

}
