package ca.ubc.cs.cpsc210.translink.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.R;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Map;

// A plotter for bus stop locations
public class BusStopPlotter extends MapViewOverlay {
    /**
     * clusterer
     */
    private RadiusMarkerClusterer stopClusterer;
    /**
     * maps each stop to corresponding marker on map
     */
    private Map<Stop, Marker> stopMarkerMap = new HashMap<>();
    /**
     * marker for stop that is nearest to user (null if no such stop)
     */
    private Marker nearestStnMarker;
    private Activity activity;
    private StopInfoWindow stopInfoWindow;


    /**
     * Constructor
     *
     * @param activity the application context
     * @param mapView  the map view on which buses are to be plotted
     */
    public BusStopPlotter(Activity activity, MapView mapView) {
        super(activity.getApplicationContext(), mapView);
        this.activity = activity;
        nearestStnMarker = null;
        stopInfoWindow = new StopInfoWindow((StopSelectionListener) activity, mapView);
        newStopClusterer();
    }

    public RadiusMarkerClusterer getStopClusterer() {
        return stopClusterer;
    }

    /**
     * Mark all visible stops in stop manager onto map.
     */
    public void markStops(Location currentLocation) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        updateVisibleArea();
        newStopClusterer();
        for (Stop s : StopManager.getInstance()) {
            if (Geometry.rectangleContainsPoint(northWest, southEast, s.getLocn())) {
                Marker marker = getMarker(s);
                if (marker != null) {
                    setMarker(s, marker);
                } else {
                    marker = makeMarker(s, Geometry.gpFromLatLon(s.getLocn()), stopIconDrawable);
                    setMarker(s, marker);
                }
                stopClusterer.add(marker);
            }
        }

    }

    //helper for markStops
    private String titleGenerator(Stop stop) {
        StringBuilder sb = new StringBuilder();
        String title = stop.getNumber() + " " + stop.getName();
        sb.append(title);
        for (Route r : stop.getRoutes()) {
            String routes = "\n" + r.getNumber();
            sb.append(routes);
        }
        return sb.toString();
    }

    /**
     * Create a new stop cluster object used to group stops that are close by to reduce screen clutter
     */
    private void newStopClusterer() {
        stopClusterer = new RadiusMarkerClusterer(activity);
        stopClusterer.getTextPaint().setTextSize(20.0F * BusesAreUs.dpiFactor());
        int zoom = mapView == null ? 16 : mapView.getZoomLevel();
        if (zoom == 0) {
            zoom = MapDisplayFragment.DEFAULT_ZOOM;
        }
        int radius = 1000 / zoom;

        stopClusterer.setRadius(radius);
        Drawable clusterIconD = activity.getResources().getDrawable(R.drawable.stop_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        stopClusterer.setIcon(clusterIcon);
    }

    /**
     * Update marker of nearest stop (called when user's location has changed).  If nearest is null,
     * no stop is marked as the nearest stop.
     *
     * @param nearest stop nearest to user's location (null if no stop within StopManager.RADIUS metres)
     */
    public void updateMarkerOfNearest(Stop nearest) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        Drawable closestStopIconDrawable = activity.getResources().getDrawable(R.drawable.closest_stop_icon);

        if (nearestStnMarker != null) {
            nearestStnMarker.setIcon(stopIconDrawable);
            nearestStnMarker = null;
        }
        if (nearest != null) {
            nearestStnMarker = getMarker(nearest);
            if (nearestStnMarker == null) {
                setupNearestMarker(nearest, closestStopIconDrawable);
            } else {
                nearestStnMarker.setIcon(closestStopIconDrawable);
            }
        }
    }

    private void setupNearestMarker(Stop s, Drawable icon) {
        GeoPoint gp = Geometry.gpFromLatLon(s.getLocn());
        nearestStnMarker = makeMarker(s, gp, icon);
        addMarker(nearestStnMarker, s);
    }

    //helper for setupNearestMarker and markStops
    private Marker makeMarker(Stop stop, GeoPoint gp, Drawable icon) {
        Marker m = new Marker(mapView);
        m.setTitle(titleGenerator(stop));
        m.setPosition(gp);
        m.setRelatedObject(stop);
        m.setInfoWindow(stopInfoWindow);
        m.setIcon(icon);
        return m;
    }

    //helper for setupNearestMarker
    private void addMarker(Marker m, Stop s) {
        setMarker(s, m);
        stopClusterer.add(m);
    }

    /**
     * Manage mapping from stops to markers using a map from stops to markers.
     * The mapping in the other direction is done using the Marker.setRelatedObject() and
     * Marker.getRelatedObject() methods.
     */
    private Marker getMarker(Stop stop) {
        return stopMarkerMap.get(stop);
    }

    private void setMarker(Stop stop, Marker marker) {
        stopMarkerMap.put(stop, marker);
    }

    private void clearMarker(Stop stop) {
        stopMarkerMap.remove(stop);
    }

    private void clearMarkers() {
        stopMarkerMap.clear();
    }
}
