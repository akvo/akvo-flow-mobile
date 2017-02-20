/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.datapoints.map;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;

/**
 * This custom renderer overrides original 'bucketed' names, in order to display the accurate
 * number of markers within a cluster.
 */
public class PointRenderer extends DefaultClusterRenderer<MapDataPoint> {

    public PointRenderer(GoogleMap map, Context context,
            ClusterManager<MapDataPoint> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(MapDataPoint item, MarkerOptions markerOptions) {
        markerOptions.title(item.getName()).snippet(item.getId());
    }

    @Override
    protected int getBucket(Cluster<MapDataPoint> cluster) {
        return cluster.getSize();
    }

    @Override
    protected String getClusterText(int bucket) {
        return String.valueOf(bucket);
    }
}
