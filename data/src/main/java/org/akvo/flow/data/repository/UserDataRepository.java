/*
 * Copyright (C) 2017-2019,2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.domain.repository.UserRepository;

import javax.inject.Inject;

import io.reactivex.Observable;

public class UserDataRepository implements UserRepository {

    private final DataSourceFactory dataSourceFactory;

    @Inject
    public UserDataRepository(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Observable<Boolean> mobileSyncAllowed() {
        return dataSourceFactory.getSharedPreferencesDataSource().mobileSyncEnabled();
    }

    @Override
    public Observable<Boolean> keepScreenOn() {
        return dataSourceFactory.getSharedPreferencesDataSource().keepScreenOn();
    }

    @Override
    public Observable<Integer> getImageSize() {
        return dataSourceFactory.getSharedPreferencesDataSource().getImageSize();
    }

    @Override
    public Observable<String> getDeviceId() {
        return dataSourceFactory.getSharedPreferencesDataSource().getDeviceId();
    }

    @Override
    public Observable<Boolean> saveScreenOnPreference(Boolean keepScreenOn) {
        return dataSourceFactory.getSharedPreferencesDataSource().saveScreenOn(keepScreenOn);
    }

    @Override
    public Observable<Boolean> saveEnableMobileDataPreference(Boolean enable) {
        return dataSourceFactory.getSharedPreferencesDataSource().saveEnableMobileData(enable);
    }

    @Override
    public Observable<Boolean> saveImageSizePreference(Integer size) {
        return dataSourceFactory.getSharedPreferencesDataSource().saveImageSize(size);
    }

    @Override
    public Observable<Long> getSelectedSurvey() {
        return dataSourceFactory.getSharedPreferencesDataSource().getSelectedSurvey();
    }

    @Override
    public Observable<Boolean> clearSelectedSurvey() {
        return dataSourceFactory.getSharedPreferencesDataSource().clearSelectedSurvey();
    }

    @Override
    public Observable<Boolean> setSelectedSurvey(long surveyGroupId) {
        return dataSourceFactory.getSharedPreferencesDataSource().setSelectedSurvey(surveyGroupId);
    }

    @Override
    public Long fetchSelectedUser() {
        return dataSourceFactory.getSharedPreferencesDataSource().getSelectedUser();
    }

    @Override
    public Observable<Boolean> clearSelectedUser() {
        return dataSourceFactory.getSharedPreferencesDataSource().clearSelectedUser();
    }

    @Override
    public Observable<Boolean> setSelectedUser(long userId) {
        return dataSourceFactory.getSharedPreferencesDataSource().setSelectedUser(userId);
    }

    @Override
    public Observable<Long> getPublishDataTime() {
        return dataSourceFactory.getSharedPreferencesDataSource().getPublishDataTime();
    }

    @Override
    public Observable<Boolean> setPublishDataTime() {
        return dataSourceFactory.getSharedPreferencesDataSource().setPublishDataTime();
    }

    @Override
    public Observable<Boolean> clearPublishDataTime() {
        return dataSourceFactory.getSharedPreferencesDataSource().clearPublishDataTime();
    }

    @Override
    public Observable<Boolean> clearUserPreferences() {
        return dataSourceFactory.getSharedPreferencesDataSource().clearUserPreferences();
    }

    @Override
    public Observable<Boolean> isDeviceSetUp() {
        return dataSourceFactory.getSharedPreferencesDataSource().isDeviceSetup();
    }

    @Override
    public Observable<Boolean> mobileUploadSet() {
        return dataSourceFactory.getSharedPreferencesDataSource().mobileUploadSet();
    }

    @Override
    public Observable<Boolean> clearAppUpdateNotified() {
        return dataSourceFactory.getSharedPreferencesDataSource().clearAppUpdateNotified();
    }

    @Override
    public Long getLastNotificationTime() {
        return dataSourceFactory.getSharedPreferencesDataSource().getAppUpdateNotifiedTime();
    }

    @Override
    public void saveLastNotificationTime() {
        dataSourceFactory.getSharedPreferencesDataSource().setAppUpdateNotifiedTime();
    }
}
