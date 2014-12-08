package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class PolygonFeature extends Feature {
    public static final String GEOMETRY_TYPE = "Polygon";

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
            polygonOptions.strokeColor(mSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
            polygonOptions.fillColor(FILL_COLOR);
            polygonOptions.add(point);// Polygon cannot be created without points
            mPolygon = mMap.addPolygon(polygonOptions);
        } else {
            mPolygon.setPoints(mPoints);
        }
    }

    @Override
    public void removePoint() {
        super.removePoint();
        if (mPoints.isEmpty()) {
            mPolygon.remove();
            mPolygon = null;
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

    @Override
    public void invalidate() {
        super.invalidate();
        if (mPolygon != null) {
            mPolygon.setStrokeColor(mSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
        }
    }

    @Override
    public String getTitle() {
        return "Area";
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

}
