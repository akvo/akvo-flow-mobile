package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    protected MarkerOptions getMarkerOptions(LatLng point) {
        return new MarkerOptions()
                .position(point)
                .title(point.toString())
                .icon(getMarkerBitmapDescriptor());
    }

    @Override
    protected BitmapDescriptor getMarkerBitmapDescriptor() {
        if (mSelected) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        }
        return BitmapDescriptorFactory.defaultMarker();
    }

    @Override
    public String getTitle() {
        return "Points";
    }

}
