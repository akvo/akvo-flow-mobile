/*
 * Copyright (C) 2016-2020 Stichting Akvo (Akvo Foundation)
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

import com.google.gson.Gson;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.datasource.preferences.SharedPreferencesDataSource;
import org.akvo.flow.data.executor.JobExecutor;
import org.akvo.flow.data.net.Encoder;
import org.akvo.flow.data.net.HmacInterceptor;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.net.RestServiceFactory;
import org.akvo.flow.data.net.S3User;
import org.akvo.flow.data.net.SignatureHelper;
import org.akvo.flow.data.net.s3.AmazonAuthHelper;
import org.akvo.flow.data.net.s3.BodyCreator;
import org.akvo.flow.data.net.s3.S3RestApi;
import org.akvo.flow.data.repository.ApkDataRepository;
import org.akvo.flow.data.repository.DataPointDataRepository;
import org.akvo.flow.data.repository.FileDataRepository;
import org.akvo.flow.data.repository.FormDataRepository;
import org.akvo.flow.data.repository.LanguagesDataRepository;
import org.akvo.flow.data.repository.MissingAndDeletedDataRepository;
import org.akvo.flow.data.repository.SetupDataRepository;
import org.akvo.flow.data.repository.SurveyDataRepository;
import org.akvo.flow.data.repository.UserDataRepository;
import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.LanguageTable;
import org.akvo.flow.database.SurveyLanguagesDataSource;
import org.akvo.flow.database.SurveyLanguagesDbDataSource;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.SchedulerCreator;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.DataPointRepository;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.FormRepository;
import org.akvo.flow.domain.repository.LanguagesRepository;
import org.akvo.flow.domain.repository.MissingAndDeletedRepository;
import org.akvo.flow.domain.repository.SetupRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.DeviceHelper;
import org.akvo.flow.domain.util.GsonMapper;
import org.akvo.flow.thread.UIThread;
import org.akvo.flow.util.logging.DebugLoggingHelper;
import org.akvo.flow.util.logging.LoggingHelper;
import org.akvo.flow.util.logging.ReleaseLoggingHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import androidx.annotation.NonNull;
import dagger.Module;
import dagger.Provides;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
public class ApplicationModule {

    private static final String SERVICE_FACTORY_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final String REST_API_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss ";
    private static final String TIMEZONE = "GMT";
    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;
    private static final int CONNECTION_TIMEOUT = 10;
    /**
     * Requests to GAE take a long time especially when there are a lot of datapoints
     */
    private static final int NO_TIMEOUT = 0;

    private final FlowApp application;

    public ApplicationModule(FlowApp application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return application;
    }

    @Provides
    @Singleton
    public ApkRepository provideApkRepository(ApkDataRepository apkDataRepository) {
        return apkDataRepository;
    }

    @Provides
    @Singleton
    public FileRepository provideFileRepository(FileDataRepository fileDataRepository) {
        return fileDataRepository;
    }

    @Provides
    @Singleton
    public LanguagesRepository provideLanguagesRepository(LanguagesDataRepository languagesRepo) {
        return languagesRepo;
    }

    @Provides
    @Singleton
    public LoggingHelper loggingHelper() {
        if (BuildConfig.DEBUG) {
            return new DebugLoggingHelper();
        } else {
            return new ReleaseLoggingHelper();
        }
    }

    @Provides
    @Singleton
    public SurveyRepository provideSurveyRepository(SurveyDataRepository surveyDataRepository) {
        return surveyDataRepository;
    }

    @Provides
    @Singleton
    public MissingAndDeletedRepository provideMissingAndDeletedRepository(
            MissingAndDeletedDataRepository repository) {
        return repository;
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository(UserDataRepository userDataRepository) {
        return userDataRepository;
    }

    @Provides
    @Singleton
    public SetupRepository provideSetupRepository(SetupDataRepository setupDataRepository) {
        return setupDataRepository;
    }

    @Provides
    @Singleton
    public FormRepository provideFormRepository(FormDataRepository formDataRepository) {
        return formDataRepository;
    }

    @Provides
    @Singleton
    public DataPointRepository provideDataPointRepository(DataPointDataRepository dataPointDataRepository) {
        return dataPointDataRepository;
    }

    @Provides
    @Singleton
    public SQLiteOpenHelper provideOpenHelper() {
        return new DatabaseHelper(application, new LanguageTable());
    }

    @Provides
    @Singleton
    public SqlBrite provideSqlBrite() {
        return new SqlBrite.Builder().build();
    }

    @Provides
    @Singleton
    public BriteDatabase provideDatabase(SqlBrite sqlBrite, SQLiteOpenHelper helper) {
        BriteDatabase db = sqlBrite.wrapDatabaseHelper(helper, Schedulers.io());
        db.setLoggingEnabled(BuildConfig.DEBUG);
        return db;
    }

    @Provides
    @Singleton
    public RestServiceFactory provideServiceFactory(Encoder encoder, SignatureHelper signatureHelper) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SERVICE_FACTORY_DATE_PATTERN,
                Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        OkHttpClient.Builder httpClient = createHttpClient(loggingInterceptor);
        httpClient.addInterceptor(
                new HmacInterceptor(BuildConfig.API_KEY, simpleDateFormat, encoder,
                        signatureHelper));
        OkHttpClient okHttpClientWithHmac = httpClient.build();

        OkHttpClient.Builder httpClient2 = createHttpClient(loggingInterceptor);
        OkHttpClient okHttpClient = httpClient2.build();

        return new RestServiceFactory(okHttpClientWithHmac, okHttpClient);
    }

    @Provides
    @Singleton
    public SharedPreferencesDataSource provideSharedPreferences(GsonMapper mapper) {
        return new SharedPreferencesDataSource(
                application.getSharedPreferences(PREFS_NAME, PREFS_MODE), mapper);
    }

    @Provides
    @Singleton
    public ThreadExecutor provideThreadExecutor(JobExecutor jobExecutor) {
        return jobExecutor;
    }

    @Provides
    @Singleton
    public SchedulerCreator provideSchedulerCreator(ThreadExecutor threadExecutor) {
        return new SchedulerCreator(threadExecutor);
    }

    @Provides
    @Singleton
    public PostExecutionThread providePostExecutionThread(UIThread uiThread) {
        return uiThread;
    }

    @Provides
    @Singleton
    public AmazonAuthHelper provideAmazonAuthHelper(SignatureHelper signatureHelper) {
        S3User s3User = new S3User(BuildConfig.AWS_BUCKET, BuildConfig.AWS_ACCESS_KEY_ID,
                BuildConfig.AWS_SECRET_KEY);
        return new AmazonAuthHelper(signatureHelper, s3User);
    }

    @Provides
    @Singleton
    public RestApi provideRestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory) {
        return new RestApi(deviceHelper, serviceFactory, BuildConfig.VERSION_NAME,
                BuildConfig.SERVER_BASE);
    }

    @Provides
    @Singleton
    public S3RestApi provideS3RestApi(RestServiceFactory serviceFactory, AmazonAuthHelper amazonAuthHelper,
            BodyCreator bodyCreator) {
        final DateFormat df = new SimpleDateFormat(REST_API_DATE_PATTERN, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        return new S3RestApi(serviceFactory, amazonAuthHelper, df, bodyCreator,
                "https://" + BuildConfig.AWS_BUCKET + ".s3.amazonaws.com");
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public SurveyLanguagesDataSource provideSurveyLanguageDataSource(Context context) {
        return new SurveyLanguagesDbDataSource(context);
    }

    @NonNull
    private OkHttpClient.Builder createHttpClient(
            HttpLoggingInterceptor loggingInterceptor) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        httpClient.readTimeout(NO_TIMEOUT, TimeUnit.SECONDS);
        return httpClient;
    }
}
