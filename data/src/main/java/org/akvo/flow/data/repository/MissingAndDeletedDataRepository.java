package org.akvo.flow.data.repository;

import android.support.annotation.VisibleForTesting;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.ApiFilesResult;
import org.akvo.flow.data.entity.FilesResultMapper;
import org.akvo.flow.data.entity.FilteredFilesResult;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.repository.MissingAndDeletedRepository;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class MissingAndDeletedDataRepository implements MissingAndDeletedRepository {

    private final RestApi restApi;
    private final FilesResultMapper filesResultMapper;
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public MissingAndDeletedDataRepository(RestApi restApi, FilesResultMapper filesResultMapper,
            DataSourceFactory dataSourceFactory) {
        this.restApi = restApi;
        this.filesResultMapper = filesResultMapper;
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Observable<Set<String>> downloadMissingAndDeleted(List<String> formIds, String deviceId) {
        return getPendingFiles(formIds, deviceId)
                .concatMap(new Function<FilteredFilesResult, Observable<Set<String>>>() {
                    @Override
                    public Observable<Set<String>> apply(final FilteredFilesResult filtered) {
                        return saveMissing(filtered.getMissingFiles())
                                .concatMap(new Function<Boolean, Observable<Set<String>>>() {
                                    @Override
                                    public Observable<Set<String>> apply(Boolean ignored) {
                                        return saveDeletedForms(filtered.getDeletedForms());
                                    }
                                });
                    }
                });
    }


    @VisibleForTesting
    Observable<FilteredFilesResult> getPendingFiles(List<String> formIds, String deviceId) {
        return restApi.getPendingFiles(formIds, deviceId)
                .map(new Function<ApiFilesResult, FilteredFilesResult>() {
                    @Override
                    public FilteredFilesResult apply(ApiFilesResult apiFilesResult) {
                        return filesResultMapper.transform(apiFilesResult);
                    }
                });
    }

    @VisibleForTesting
    Observable<Boolean> saveMissing(final Set<String> missingFiles) {
        final DatabaseDataSource dataSource = dataSourceFactory.getDataBaseDataSource();
        return dataSource.saveMissingFiles(missingFiles)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean aBoolean) {
                        return dataSource.updateFailedTransmissionsSurveyInstances(missingFiles);
                    }
                });
    }

    @VisibleForTesting
    Observable<Set<String>> saveDeletedForms(Set<String> deletedForms) {
        return dataSourceFactory.getDataBaseDataSource().setDeletedForms(deletedForms);
    }
}
