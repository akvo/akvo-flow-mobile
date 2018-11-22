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

package org.akvo.flow.domain.interactor.apk;

import android.os.Build;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.repository.ApkRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;

public class GetApkData extends UseCase {

    public static final String APP_VERSION_PARAM = "version";

    private final ApkRepository apkRepository;

    @Inject
    public GetApkData(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            ApkRepository apkRepository) {
        super(threadExecutor, postExecutionThread);
        this.apkRepository = apkRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(final Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(APP_VERSION_PARAM)) {
            throw new IllegalArgumentException("Missing params");
        }
        return apkRepository.loadApkData(Build.VERSION.SDK_INT + "");
    }
}
