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

import com.getsentry.raven.android.AndroidRavenFactory;
import com.getsentry.raven.dsn.Dsn;

public class CustomAndroidRavenFactory extends AndroidRavenFactory {

    private final Context applicationContext;

    public CustomAndroidRavenFactory(Context applicationContext) {
        super(applicationContext);
        this.applicationContext = applicationContext;
    }

    @Override
    public com.getsentry.raven.Raven createRavenInstance(Dsn dsn) {
        com.getsentry.raven.Raven ravenInstance = super.createRavenInstance(dsn);
        ravenInstance.addBuilderHelper(new CustomEventBuilderHelper(applicationContext));
        return ravenInstance;
    }


}
