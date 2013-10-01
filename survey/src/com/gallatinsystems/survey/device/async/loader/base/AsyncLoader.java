package com.gallatinsystems.survey.device.async.loader.base;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class AsyncLoader<T> extends AsyncTaskLoader<T> {
    private T mData;

    public AsyncLoader(Context context) {
        super(context);
    }
    
    @Override
    public void deliverResult(T data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        this.mData = data;
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        mData = null;
    }
}
