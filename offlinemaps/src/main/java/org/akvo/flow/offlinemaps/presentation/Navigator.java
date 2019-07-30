/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.offlinemaps.presentation;

import android.content.Context;
import android.content.Intent;

import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.domain.entity.MapInfo;
import org.akvo.flow.offlinemaps.presentation.download.OfflineMapDownloadActivity;
import org.akvo.flow.offlinemaps.presentation.list.OfflineAreasListActivity;
import org.akvo.flow.offlinemaps.presentation.view.OfflineAreaViewActivity;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class Navigator {

    @Inject
    public Navigator() {
    }

    public void navigateToViewOffline(@Nullable Context context, String mapName, MapInfo mapInfo) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineAreaViewActivity.class);
            intent.putExtra(OfflineAreaViewActivity.NAME_EXTRA, mapName);
            intent.putExtra(OfflineAreaViewActivity.MAP_INFO_EXTRA, mapInfo);
            context.startActivity(intent);
        }
    }

    public void navigateToOfflineMapAreasCreation(@Nullable Context context, int callingActivity) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineMapDownloadActivity.class);
            intent.putExtra(Constants.CALLING_SCREEN_EXTRA, callingActivity);
            context.startActivity(intent);
        }
    }

    public void navigateToOfflineAreasList(@Nullable Context context) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineAreasListActivity.class);
            context.startActivity(intent);
        }
    }
}
