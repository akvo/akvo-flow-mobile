/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.repository;

import io.reactivex.Observable;

public interface UserRepository {

    Observable<Boolean> mobileSyncAllowed();

    Observable<Boolean> keepScreenOn();

    Observable<Integer> getImageSize();

    Observable<String> getDeviceId();

    Observable<Boolean> saveScreenOnPreference(Boolean keepScreenOn);

    Observable<Boolean> saveEnableMobileDataPreference(Boolean enable);

    Observable<Boolean> saveImageSizePreference(Integer size);

    Observable<Long> getSelectedSurvey();

    Observable<Boolean> clearSelectedSurvey();

    Observable<Boolean> setSelectedSurvey(long surveyGroupId);

    Long getSelectedUser();

    Observable<Boolean> clearSelectedUser();

    Observable<Boolean> setSelectedUser(long userId);

    Observable<Long> getPublishDataTime();

    Observable<Boolean> setPublishDataTime();

    Observable<Boolean> clearPublishDataTime();

    Observable<Boolean> clearUserPreferences();

    Observable<Boolean> isDeviceSetUp();

    Observable<Boolean> mobileUploadSet();

    Observable<Boolean> clearAppUpdateNotified();

    Observable<Long> getLastNotificationTime();

    Observable<Boolean> saveLastNotificationTime();
}
