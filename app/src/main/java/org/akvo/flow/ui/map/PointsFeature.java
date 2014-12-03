package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PointsFeature extends Feature {

    public PointsFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
    }

    @Override
    public void drawPoint(LatLng point) {
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(point.toString()));
    }

}
