/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {
    protected static final int POINT_SIZE_DEFAULT = 40;// Default marker size (px).
    protected static final int POINT_SIZE_SELECTED = 60;// Selected marker size (px).

    protected static final int POINT_COLOR_DEFAULT = 0xEE736357;
    protected static final int POINT_COLOR_ACTIVE = 0xFFE27C00;
    protected static final int POINT_COLOR_SELECTED = 0xFF00A79D;
    protected static final int POINT_COLOR_FILL = 0x55FFFFFF;

    protected static final int STROKE_COLOR_DEFAULT = 0xEE736357;
    protected static final int STROKE_COLOR_SELECTED = 0xFF736357;

    protected boolean mSelected;
    protected Marker mSelectedMarker;

    protected GoogleMap mMap;
    protected List<LatLng> mPoints;
    protected List<Marker> mMarkers;
    protected List<Property> mProperties;

    private static final BitmapDescriptor MARKER_DISABLED;
    private static final BitmapDescriptor MARKER_ENABLED;
    private static final BitmapDescriptor MARKER_SELECTED;
    private static final BitmapDescriptor MARKER_HIGHLIGHTED;

    static {
        MARKER_DISABLED = getMarkerBitmapDescriptor(PointStatus.DISABLED);
        MARKER_ENABLED = getMarkerBitmapDescriptor(PointStatus.ENABLED);
        MARKER_SELECTED = getMarkerBitmapDescriptor(PointStatus.SELECTED);
        MARKER_HIGHLIGHTED = getMarkerBitmapDescriptor(PointStatus.HIGHLIGHTED);
    }

    private enum PointStatus {
        DISABLED, // Unselected Feature. Normal mode.
        ENABLED, // Selected Feature, but marker is not selected (just 'active').
        SELECTED, // Currently selected marker.
        HIGHLIGHTED // Highlighted point, used to show the descendant of the selected point.
    }

    public Feature(GoogleMap map) {
        mMap = map;
        mPoints = new ArrayList<>();
        mMarkers = new ArrayList<>();
        mProperties = new ArrayList<>();
    }

    public abstract int getTitle();
    public abstract String geoGeometryType();
    public abstract boolean highlightNext(int position);

    public boolean contains(Marker marker) {
        return mMarkers.contains(marker);
    }

    public List<LatLng> getPoints() {
        return mPoints;
    }

    /**
     * Add point to the map. A new marker will be created based on the point,
     * and the underlying overlays recomputed.
     * @param point LatLng value of the new point.
     */
    public void addPoint(LatLng point) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(String.format("lat/lng: %.5f, %.5f", point.latitude, point.longitude))
                .anchor(0.5f, 0.5f)
                .draggable(false)
                .icon(MARKER_DISABLED));

        // Insert new point just after the currently selected marker (if any)
        if (mSelectedMarker != null) {
            int index =  mMarkers.indexOf(mSelectedMarker) + 1;
            mMarkers.add(index, marker);
            mPoints.add(index, point);
        } else {
            mMarkers.add(marker);
            mPoints.add(point);
        }

        setSelected(mSelected, marker);
    }

    /**
     * Delete selected point.
     */
    public void removePoint() {
        if (mSelectedMarker == null) {
            return;
        }

        int index =  mMarkers.indexOf(mSelectedMarker);
        mSelectedMarker.remove();
        mPoints.remove(index);
        mMarkers.remove(index);
    }

    /**
     * Delete the whole feature from the map.
     */
    public void delete() {
        for (Marker marker : mMarkers) {
            marker.remove();
        }
        mMarkers.clear();
        mPoints.clear();
    }

    public void onDrag(Marker marker) {
        int index =  mMarkers.indexOf(marker);
        if (index == -1) {
            return;
        }

        mPoints.remove(index);
        mPoints.add(index, marker.getPosition());
        invalidate();
    }

    public void setSelected(boolean selected, Marker marker) {
        if (mSelectedMarker != null) {
            mSelectedMarker.setDraggable(false);
        }

        mSelected = selected;
        mSelectedMarker = selected ? marker: null;

        if (mSelectedMarker != null) {
            mSelectedMarker.setDraggable(true);
        }
        invalidate();
    }

    /**
     * Recompute geoshape and redraw the corresponding markers. Subclasses should add any extra
     * step to this process by overriding this method.
     */
    protected void invalidate() {
        // Recompute icons, depending on point status
        long selected = -1, next = -1;
        if (mSelected && mSelectedMarker != null && mMarkers.contains(mSelectedMarker)) {
            selected = mMarkers.indexOf(mSelectedMarker);
            next = selected > -1 ? (selected + 1) % mMarkers.size() : mMarkers.size() - 1;
        }

        for (int i=0; i<mMarkers.size(); i++) {
            Marker marker = mMarkers.get(i);
            if (!mSelected) {
                marker.setIcon(MARKER_DISABLED);
            } else if (i == selected) {
                marker.setIcon(MARKER_SELECTED);
                marker.showInfoWindow();
            } else if (i == next && highlightNext(i)) {
                marker.setIcon(MARKER_HIGHLIGHTED);
            } else {
                marker.setIcon(MARKER_ENABLED);
            }
        }

        // Compute properties
        mProperties.clear();
        String count = String.valueOf(mMarkers.size());
        mProperties.add(new Property("pointCount", count, "Point Count", count));
    }

    public List<Property> getProperties() {
        return mProperties;
    }

    public void load(List<LatLng> points) {
        for (LatLng point : points) {
            addPoint(point);
        }
    }

    /**
     * Programmatically creates Bitmap based on point status
     * @param status PointStatus of the feature point.
     * @return BitmapDescriptor of the newly created Bitmap.
     */
    protected static BitmapDescriptor getMarkerBitmapDescriptor(PointStatus status) {
        int size, color;
        switch (status) {
            case SELECTED:
                size = POINT_SIZE_SELECTED;
                color = POINT_COLOR_SELECTED;
                break;
            case HIGHLIGHTED:
                size = POINT_SIZE_DEFAULT;
                color = POINT_COLOR_SELECTED;
                break;
            case ENABLED:
                size = POINT_SIZE_DEFAULT;
                color = POINT_COLOR_ACTIVE;
                break;
            case DISABLED:
            default:
                size = POINT_SIZE_DEFAULT;
                color = POINT_COLOR_DEFAULT;
        }

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint solid = new Paint();
        solid.setColor(color);
        solid.setAntiAlias(true);
        Paint fill = new Paint();
        fill.setAntiAlias(true);
        fill.setColor(POINT_COLOR_FILL);

        final float center = size / 2f;
        canvas.drawCircle(center, center, center, solid);// Outer circle
        canvas.drawCircle(center, center, center * 0.9f, fill);// Fill circle
        canvas.drawCircle(center, center, center * 0.25f, solid);// Inner circle

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    public static class Property {
        public String mKey;
        public String mValue;
        public String mDisplayName;
        public String mDisplayValue;

        public Property(String key, String value, String displayName, String displayValue) {
            mKey = key;
            mValue = value;
            mDisplayName = displayName;
            mDisplayValue = displayValue;
        }
    }

}
