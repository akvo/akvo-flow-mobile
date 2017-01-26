/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.fragment;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import org.akvo.flow.domain.SurveyedLocale;

import java.lang.ref.WeakReference;

/**
 * This custom renderer overrides original 'bucketed' names, in order to display the accurate
 * number of markers within a cluster.
 */
public class PointRenderer extends DefaultClusterRenderer<SurveyedLocale> {

    private final WeakReference<Context> contextWeakReference;

    public PointRenderer(GoogleMap map, Context context,
            ClusterManager<SurveyedLocale> clusterManager) {
        super(context, map, clusterManager);
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected void onBeforeClusterItemRendered(SurveyedLocale item,
            MarkerOptions markerOptions) {
        Context context = contextWeakReference.get();
        if (context != null) {
            markerOptions.title(item.getDisplayName(context)).snippet(item.getId());
        }
    }

    @Override
    protected int getBucket(Cluster<SurveyedLocale> cluster) {
        return cluster.getSize();
    }

    @Override
    protected String getClusterText(int bucket) {
        return String.valueOf(bucket);
    }
}
