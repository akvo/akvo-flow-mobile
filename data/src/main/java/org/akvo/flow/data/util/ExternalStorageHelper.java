/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.data.util;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import androidx.annotation.Nullable;

import javax.inject.Inject;

import timber.log.Timber;

public class ExternalStorageHelper {

    private static final double ONE_MEBIBIT = 1048576.0;

    @Inject
    public ExternalStorageHelper() {
    }

    @Nullable
    public String getExternalStoragePath() {
        if (isExternalStorageMounted()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            return null;
        }
    }

    public long getExternalStorageAvailableSpaceInMb() {
        if (!isExternalStorageMounted()) {
            Timber.e("External storage is not mounted");
            return 0L;
        }
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = getAvailableSpace(stat);
        return (long) Math.floor(sdAvailSize / ONE_MEBIBIT);
    }

    private boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private double getAvailableSpace(StatFs stat) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
        } else {
            //noinspection deprecation
            return (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        }
    }
}
