/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.entity;

import androidx.annotation.Nullable;

import org.akvo.flow.domain.entity.ApkData;

import javax.inject.Inject;

/**
 * Maps data ApkData object to domain ApkData object
 */
public class ApkDataMapper {

    @Inject
    public ApkDataMapper() {
    }

    @Nullable
    public ApkData transform(@Nullable ApiApkData apiApkData) {
        if (apiApkData == null) {
            return null;
        }
        return new ApkData(apiApkData.getVersion(), apiApkData.getFileUrl(), apiApkData.getMd5Checksum());
    }
}
