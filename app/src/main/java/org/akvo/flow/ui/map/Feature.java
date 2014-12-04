package org.akvo.flow.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
    private static final int POINT_SIZE = 20;

    protected GoogleMap mMap;
    protected List<LatLng> mPoints;
    protected List<Marker> mMarkers;

    public Feature(GoogleMap map) {
        mMap = map;
        mPoints = new ArrayList<LatLng>();
        mMarkers = new ArrayList<Marker>();
    }

    public void addPoint(LatLng point) {
        mPoints.add(point);
        mMarkers.add(mMap.addMarker(getMarkerOptions(point)));
    }

    public void delete() {
        for (Marker marker : mMarkers) {
            marker.remove();
        }
    }

    protected MarkerOptions getMarkerOptions(LatLng point) {
        Bitmap bmp = Bitmap.createBitmap(POINT_SIZE, POINT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint color = new Paint();
        color.setColor(Color.BLACK);

        float center = POINT_SIZE / 2f;

        canvas.drawCircle(center, center, center, color);

        return new MarkerOptions()
                .position(point)
                .title(point.toString())
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp));
    }

}
