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

package org.akvo.flow.presentation.datapoints.map;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.maps.Constants;
import org.akvo.flow.presentation.datapoints.DisplayNameMapper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FeatureMapper {

    private final DisplayNameMapper displayNameMapper;

    @Inject
    public FeatureMapper(DisplayNameMapper displayNameMapper) {
        this.displayNameMapper = displayNameMapper;
    }

    FeatureCollection getFeatureCollection(List<DataPoint> dataPoints) {
        List<Feature> features = new ArrayList<>();
        for (DataPoint item : dataPoints) {
            Feature feature = getFeature(item);
            if (feature != null) {
                features.add(feature);
            }
        }
        return FeatureCollection.fromFeatures(features);
    }

    public Feature getFeature(DataPoint item) {
        Double longitude = item.getLongitude();
        Double latitude = item.getLatitude();
        if (latitude != null && longitude != null) {
            Feature feature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude));
            feature.addStringProperty(Constants.ID_PROPERTY, item.getId());
            feature.addStringProperty(Constants.NAME_PROPERTY,
                    displayNameMapper.createDisplayName(item.getName()));
            feature.addNumberProperty(Constants.LATITUDE_PROPERTY, latitude);
            feature.addNumberProperty(Constants.LONGITUDE_PROPERTY, longitude);
            return feature;
        }
        return null;
    }
}
