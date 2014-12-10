package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class PointsFeature extends Feature {
    public static final String GEOMETRY_TYPE = "MultiPoint";

    public PointsFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
    }

    @Override
    public String getTitle() {
        return "Points";
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

}
