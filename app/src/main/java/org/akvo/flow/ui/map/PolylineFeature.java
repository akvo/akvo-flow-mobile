package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class PolylineFeature extends Feature {
    private Polyline mPolyline;

    public PolylineFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
        if (mPolyline == null) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.color(mSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
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
            mPolyline.setColor(mSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
        }
    }

    @Override
    public String getTitle() {
        return "Line";
    }

}
