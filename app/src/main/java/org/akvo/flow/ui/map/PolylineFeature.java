package org.akvo.flow.ui.map;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.akvo.flow.R;
import org.akvo.flow.util.GeoUtil;

public class PolylineFeature extends Feature {
    public static final String GEOMETRY_TYPE = "LineString";

    private Polyline mPolyline;

    public PolylineFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
        if (mPolyline == null) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.color(mSelected ? STROKE_COLOR_SELECTED : STROKE_COLOR_DEFAULT);
            mPolyline = mMap.addPolyline(polylineOptions);
        }
        mPolyline.setPoints(mPoints);
    }

    @Override
    public void removePoint() {
        super.removePoint();
        mPolyline.setPoints(mPoints);
    }

    @Override
    public void delete() {
        super.delete();
        if (mPolyline != null) {
            mPolyline.remove();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (mPolyline != null) {
            mPolyline.setColor(mSelected ? STROKE_COLOR_SELECTED : STROKE_COLOR_DEFAULT);
            mPolyline.setPoints(mPoints);
        }

        // Compute line length
        float length = 0f;
        LatLng previous = null;
        for (LatLng point : mPoints) {
            if (previous != null) {
                float[] distance = new float[1];
                Location.distanceBetween(previous.latitude, previous.longitude, point.latitude, point.longitude, distance);
                length += distance[0];
            }
            previous = point;
        }
        String lengthVal = String.format("%.2f", length);
        mProperties.add(new Property("length", lengthVal, "Length", GeoUtil.getDisplayLength(length)));
    }

    @Override
    public int getTitle() {
        return R.string.geoshape_line;
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

    @Override
    public boolean highlightPrevious(int position) {
        return position < mMarkers.size() - 1;// Do not highlight polyline tail
    }

}
