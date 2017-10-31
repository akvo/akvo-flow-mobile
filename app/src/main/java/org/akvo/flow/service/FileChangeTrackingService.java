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

package org.akvo.flow.service;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;

public class FileChangeTrackingService extends Service {

    public FileObserver fileObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fileObserver = new FileObserver(
                FileUtil.getFilesDir(FileUtil.FileType.INBOX).getAbsolutePath(),
                FileObserver.CREATE) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                Intent bootStrapIntent = new Intent(ConstantUtil.BOOTSTRAP_INTENT);
                sendBroadcast(bootStrapIntent);
            }
        };
        fileObserver.startWatching();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
    }
}
