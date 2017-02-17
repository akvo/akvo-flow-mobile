/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.logging;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;

import timber.log.Timber;

public abstract class LoggingHelper {

    LoggingHelper() {
    }

    public abstract void initSentry();

    public void initDebugTree() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Nullable
    String getSentryDsn(Resources resources) {
        final PropertyUtil props = new PropertyUtil(resources);
        String sentryDsn = props.getProperty(ConstantUtil.SENTRY_DSN);
        return sentryDsn;
    }
}
