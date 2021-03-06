package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import android.graphics.Canvas;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.RoutePattern;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A bus route drawer
public class BusRouteDrawer extends MapViewOverlay {
    /**
     * overlay used to display bus route legend text on a layer above the map
     */
    private BusRouteLegendOverlay busRouteLegendOverlay;
    /**
     * overlays used to plot bus routes
     */
    private List<Polyline> busRouteOverlays;

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */
    public BusRouteDrawer(Context context, MapView mapView) {
        super(context, mapView);
        busRouteLegendOverlay = createBusRouteLegendOverlay();
        busRouteOverlays = new ArrayList<>();
    }

    /**
     * Plot each visible segment of each route pattern of each route going through the selected stop.
     */
    public void plotRoutes(int zoomLevel) {
        busRouteLegendOverlay.clear();
        busRouteOverlays.clear();
        updateVisibleArea();
        Stop currentStop = StopManager.getInstance().getSelected();
        if (currentStop != null) {
            for (Route r : currentStop.getRoutes()) {
                for (RoutePattern rp : r.getPatterns()) {
                    List<GeoPoint> geoPoints = new ArrayList<>();
                    List<LatLon> coords = rp.getPath();
                    addGeoPointsWithinRectangle(geoPoints, coords);
                    Polyline p = setPolyline(geoPoints, zoomLevel, r);
                    busRouteOverlays.add(p);
                }
            }
        }


    }

    //helper for plotRoutes
    private Polyline setPolyline(List<GeoPoint> geoPoints, int zoomLevel, Route r) {
        Polyline p = new Polyline(context);
        p.setWidth(getLineWidth(zoomLevel));
        p.setColor(busRouteLegendOverlay.add(r.getNumber()));
        p.setPoints(geoPoints);
        return p;
    }

    //helper for plotRoutes
    private void addGeoPointsWithinRectangle(List<GeoPoint> geoPoints, List<LatLon> coords) {
        for (int i = 0; i < coords.size() - 1; i++) {
            if (Geometry.rectangleContainsPoint(northWest, southEast, coords.get(i))
                    && Geometry.rectangleContainsPoint(northWest, southEast, coords.get(i + 1))) {

                if (Geometry.rectangleIntersectsLine(northWest, southEast, coords.get(i), coords.get(i + 1))) {
                    geoPoints.add(Geometry.gpFromLatLon(coords.get(i)));
                    geoPoints.add(Geometry.gpFromLatLon(coords.get(i + 1)));
                }
            }
        }
    }

    public List<Polyline> getBusRouteOverlays() {
        return Collections.unmodifiableList(busRouteOverlays);
    }

    public BusRouteLegendOverlay getBusRouteLegendOverlay() {
        return busRouteLegendOverlay;
    }


    /**
     * Create text overlay to display bus route colours
     */
    private BusRouteLegendOverlay createBusRouteLegendOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);
        return new BusRouteLegendOverlay(rp, BusesAreUs.dpiFactor());
    }

    /**
     * Get width of line used to plot bus route based on zoom level
     *
     * @param zoomLevel the zoom level of the map
     * @return width of line used to plot bus route
     */
    private float getLineWidth(int zoomLevel) {
        if (zoomLevel > 14) {
            return 7.0f * BusesAreUs.dpiFactor();
        } else if (zoomLevel > 10) {
            return 5.0f * BusesAreUs.dpiFactor();
        } else {
            return 2.0f * BusesAreUs.dpiFactor();
        }
    }
}
