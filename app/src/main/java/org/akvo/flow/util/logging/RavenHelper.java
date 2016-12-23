/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;

import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.android.Raven;
import com.getsentry.raven.dsn.Dsn;

import org.akvo.flow.util.PropertyUtil;

import timber.log.Timber;

public class RavenHelper extends LoggingHelper {

    public RavenHelper(Context context) {
        super(context);
    }

    @Override
    public void initSentry() {
        addTags();
        final PropertyUtil props = new PropertyUtil(context.getResources());
        String sentryDsn = getSentryDsn(props);
        RavenFactory.registerFactory(new FlowAndroidRavenFactory(context, tags));
        Raven.init(context, new Dsn(sentryDsn));
        RavenFactory.registerFactory(new FlowAndroidRavenFactory(context, tags));
    }

    @Override
    public void plantTimberTree() {
        Timber.plant(new RavenTree());
    }
}
