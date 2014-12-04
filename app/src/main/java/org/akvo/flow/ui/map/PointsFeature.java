package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class PointsFeature extends Feature {

    public PointsFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
    }

    @Override
    public MarkerOptions getMarkerOptions(LatLng point) {
        return new MarkerOptions()
                .position(point)
                .title(point.toString());
    }

}
