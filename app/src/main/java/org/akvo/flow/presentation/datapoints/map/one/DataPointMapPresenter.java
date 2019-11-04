/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.presentation.datapoints.map.one;

import com.mapbox.geojson.Feature;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.interactor.datapoints.GetDataPoint;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.DisplayNameMapper;
import org.akvo.flow.presentation.datapoints.map.FeatureMapper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class DataPointMapPresenter implements Presenter {

    private final GetDataPoint getDataPoint;
    private final FeatureMapper featureMapper;
    private final DisplayNameMapper displayNameMapper;

    private DataPointMapView view;

    @Inject
    public DataPointMapPresenter(GetDataPoint getDataPoint, FeatureMapper featureMapper,
            DisplayNameMapper displayNameMapper) {
        this.getDataPoint = getDataPoint;
        this.featureMapper = featureMapper;
        this.displayNameMapper = displayNameMapper;
    }

    public void setView(DataPointMapView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        getDataPoint.dispose();
    }

    public void loadDataPoint(String dataPointId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(GetDataPoint.PARAM_DATA_POINT_ID, dataPointId);
        getDataPoint.execute(new DisposableSingleObserver<DataPoint>() {
            @Override
            public void onSuccess(DataPoint dataPoint) {
                Feature feature = featureMapper.getFeature(dataPoint);
                if (feature != null) {
                    String displayName = displayNameMapper.createDisplayName(dataPoint.getName());
                    view.showDataPoint(displayName, feature);
                } else {
                    onDataPointError();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                onDataPointError();
            }
        }, params);
    }

    private void onDataPointError() {
        view.showDataPointError();
        view.dismiss();
    }
}
