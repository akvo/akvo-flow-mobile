package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class PolygonFeature extends Feature {
    private static final int FILL_COLOR = 0xAA77AFFF;
    private Polygon mPolygon;

    public PolygonFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
        if (mPolygon == null) {
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.fillColor(FILL_COLOR);
            polygonOptions.add(point);// Polygon cannot be created without points
            mPolygon = mMap.addPolygon(polygonOptions);
        } else {
            mPolygon.setPoints(mPoints);
        }
    }

    @Override
    public void delete() {
        super.delete();
        if (mPolygon != null) {
            mPolygon.remove();
        }
    }

}
