/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.os.Environment;
import android.os.StatFs;

import timber.log.Timber;

public class StorageHelper {

    public long getExternalStorageAvailableSpace() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // TODO: more specific warning if card not mounted?
            Timber.e("External storage is not mounted");
        }
        // compute space left
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double) stat.getAvailableBlocks()
                * (double) stat.getBlockSize();
        // One binary gigabyte equals 1,073,741,824 bytes.
        // double gigaAvailable = sdAvailSize / 1073741824;
        // One binary megabyte equals 1 048 576 bytes.
        return (long) Math.floor(sdAvailSize / 1048576.0);
    }
}
