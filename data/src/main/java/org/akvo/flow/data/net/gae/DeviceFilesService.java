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

package org.akvo.flow.data.net.gae;

import org.akvo.flow.data.entity.ApiFilesResult;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static org.akvo.flow.data.util.ApiUrls.ANDROID_ID;
import static org.akvo.flow.data.util.ApiUrls.DEVICE_ID;
import static org.akvo.flow.data.util.ApiUrls.DEVICE_NOTIFICATION;
import static org.akvo.flow.data.util.ApiUrls.FORM_IDS;
import static org.akvo.flow.data.util.ApiUrls.IMEI;
import static org.akvo.flow.data.util.ApiUrls.PHONE_NUMBER;
import static org.akvo.flow.data.util.ApiUrls.VERSION;

public interface DeviceFilesService {

    @GET(DEVICE_NOTIFICATION)
    @Headers("Cache-Control: no-cache")
    Observable<ApiFilesResult> getFilesLists(@Query(PHONE_NUMBER) String phoneNumber,
            @Query(ANDROID_ID) String androidId,
            @Query(IMEI) String imei,
            @Query(VERSION) String version,
            @Query(DEVICE_ID) String deviceId,
            @Query(FORM_IDS) List<String> formIds);
}
