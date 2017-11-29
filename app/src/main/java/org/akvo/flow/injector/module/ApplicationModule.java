/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.injector.module;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.datasource.preferences.SharedPreferencesDataSource;
import org.akvo.flow.data.executor.JobExecutor;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.net.Encoder;
import org.akvo.flow.data.net.RestServiceFactory;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.data.repository.FileDataRepository;
import org.akvo.flow.data.repository.SurveyDataRepository;
import org.akvo.flow.data.repository.UserDataRepository;
import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.LanguageTable;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.thread.UIThread;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.logging.FlowAndroidRavenFactory;
import org.akvo.flow.util.logging.LoggingHelper;
import org.akvo.flow.util.logging.LoggingSendPermissionVerifier;
import org.akvo.flow.util.logging.RavenEventBuilderHelper;
import org.akvo.flow.util.logging.ReleaseLoggingHelper;
import org.akvo.flow.util.logging.TagsFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.schedulers.Schedulers;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
public class ApplicationModule {

    public static final String DATA_PATTERN = "yyyy/MM/dd HH:mm:ss";
    public static final String TIMEZONE = "GMT";
    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;

    private final FlowApp application;

    public ApplicationModule(FlowApp application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return application;
    }

    @Provides
    @Singleton
    FileRepository provideFileRepository(FileDataRepository fileDataRepository) {
        return fileDataRepository;
    }

    @Provides
    @Singleton
    LoggingHelper loggingHelper() {
//        if (BuildConfig.DEBUG) {
//            return new DebugLoggingHelper();
//        } else {
            LoggingSendPermissionVerifier loggingSendPermissionVerifier =
                    new LoggingSendPermissionVerifier(new ConnectivityStateManager(application),
                            new Prefs(application));
            RavenEventBuilderHelper loggingEventBuilderHelper
                    = new RavenEventBuilderHelper(new TagsFactory(application).getTags());
            FlowAndroidRavenFactory flowAndroidRavenFactory = new FlowAndroidRavenFactory(
                    application, loggingSendPermissionVerifier, loggingEventBuilderHelper);
            return new ReleaseLoggingHelper(application, flowAndroidRavenFactory);
//        }
    }

    @Provides
    @Singleton
    SurveyRepository provideSurveyRepository(SurveyDataRepository surveyDataRepository) {
        return surveyDataRepository;
    }

    @Provides
    @Singleton
    UserRepository provideUserRepository(UserDataRepository userDataRepository) {
        return userDataRepository;
    }

    @Provides
    @Singleton
    SQLiteOpenHelper provideOpenHelper() {
        return new DatabaseHelper(application, new LanguageTable(),
                new FlowMigrationListener(new Prefs(application),
                        new MigrationLanguageMapper(application)));
    }

    @Provides
    @Singleton
    SqlBrite provideSqlBrite() {
        return new SqlBrite.Builder().build();
    }

    @Provides
    @Singleton
    BriteDatabase provideDatabase(SqlBrite sqlBrite, SQLiteOpenHelper helper) {
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(helper, Schedulers.io());
        db.setLoggingEnabled(false);
        return db;
    }

    @Provides
    @Singleton
    RestServiceFactory provideServiceFactory() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATA_PATTERN, Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        return new RestServiceFactory(loggingInterceptor, simpleDateFormat, new Encoder(),
                BuildConfig.API_KEY, BuildConfig.SERVER_BASE);
    }

    @Provides
    @Singleton
    SharedPreferencesDataSource provideSharedPreferences() {
        return new SharedPreferencesDataSource(
                application.getSharedPreferences(PREFS_NAME, PREFS_MODE));
    }

    @Provides
    @Singleton
    ThreadExecutor provideThreadExecutor(JobExecutor jobExecutor) {
        return jobExecutor;
    }

    @Provides
    @Singleton
    PostExecutionThread providePostExecutionThread(UIThread uiThread) {
        return uiThread;
    }
}
