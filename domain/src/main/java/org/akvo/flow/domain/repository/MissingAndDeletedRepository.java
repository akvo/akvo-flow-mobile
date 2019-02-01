package org.akvo.flow.domain.repository;

import java.util.List;
import java.util.Set;

import io.reactivex.Observable;

public interface MissingAndDeletedRepository {
    Observable<Set<String>> downloadMissingAndDeleted(List<String> formIds, String deviceId);
}
