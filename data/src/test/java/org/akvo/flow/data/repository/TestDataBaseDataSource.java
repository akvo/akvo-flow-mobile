package org.akvo.flow.data.repository;

import android.support.annotation.Nullable;

import org.akvo.flow.data.datasource.DatabaseDataSource;

import java.util.Set;

import io.reactivex.Observable;

public class TestDataBaseDataSource extends DatabaseDataSource {

    public TestDataBaseDataSource() {
        super(null, null);
    }

    @Override
    public Observable<Boolean> saveMissingFiles(Set<String> missingFiles) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> updateFailedTransmissionsSurveyInstances(
            @Nullable Set<String> filenames) {
        return Observable.just(true);
    }

    @Override
    public Observable<Set<String>> setDeletedForms(@Nullable Set<String> deletedFormIds) {
        return Observable.just(deletedFormIds);
    }
}
