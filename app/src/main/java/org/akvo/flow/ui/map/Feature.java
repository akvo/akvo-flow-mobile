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
    protected static final int POINT_SIZE = 20;
    protected static final int SELECTED_COLOR = Color.BLUE;
    protected static final int UNSELECTED_COLOR = Color.BLACK;

    protected boolean mSelected;
    protected Marker mSelectedMarker;

    protected GoogleMap mMap;
    protected List<LatLng> mPoints;
    protected List<Marker> mMarkers;

    public Feature(GoogleMap map) {
        mMap = map;
        mPoints = new ArrayList<LatLng>();
        mMarkers = new ArrayList<Marker>();
    }

    public abstract String getTitle();
    public abstract String geoGeometryType();

    public boolean contains(Marker marker) {
        return mMarkers.contains(marker);
    }

    public List<LatLng> getPoints() {
        return mPoints;
    }

    public void addPoint(LatLng point) {
        Marker marker = mMap.addMarker(getMarkerOptions(point));

        // Insert new point just after the currently selected marker (if any)
        if (mSelectedMarker != null) {
            int index =  mMarkers.indexOf(mSelectedMarker) + 1;
            mMarkers.add(index, marker);
            mPoints.add(index, point);
        } else {
            mMarkers.add(marker);
            mPoints.add(point);
        }

        mSelectedMarker = marker;
        if (mSelected) {
            marker.showInfoWindow();
        }
    }

    /**
     * Delete selected point
     */
    public void removePoint() {
        if (mSelectedMarker == null) {
            return;
        }

        int index =  mMarkers.indexOf(mSelectedMarker);
        mSelectedMarker.remove();
        mPoints.remove(index);
        mMarkers.remove(index);
    }

    public void delete() {
        for (Marker marker : mMarkers) {
            marker.remove();
        }
        mMarkers.clear();
        mPoints.clear();
    }

    public void setSelected(boolean selected, Marker marker) {
        mSelected = selected;
        mSelectedMarker = selected ? marker: null;
        invalidate();
    }

    protected void invalidate() {
        // Recompute icons, depending on selection status
        for (Marker marker : mMarkers) {
            marker.setIcon(getMarkerBitmapDescriptor());
        }
    }

    protected MarkerOptions getMarkerOptions(LatLng point) {
        return new MarkerOptions()
                .position(point)
                .title(point.toString())
                .anchor(0.5f, 0.5f)
                .icon(getMarkerBitmapDescriptor());
    }

    protected BitmapDescriptor getMarkerBitmapDescriptor() {
        Bitmap bmp = Bitmap.createBitmap(POINT_SIZE, POINT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint color = new Paint();
        color.setColor(mSelected ? SELECTED_COLOR : UNSELECTED_COLOR);

        float center = POINT_SIZE / 2f;

        canvas.drawCircle(center, center, center, color);
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    public void load(List<LatLng> points) {
        for (LatLng point : points) {
            addPoint(point);
        }
    }

}
