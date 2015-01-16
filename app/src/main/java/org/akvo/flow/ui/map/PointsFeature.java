package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.akvo.flow.R;

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
    public int getTitle() {
        return R.string.geoshape_points;
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

    @Override
    public boolean highlightNext(int position) {
        return false;
    }

}
