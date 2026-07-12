package com.transitops.simulation.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

    public String getRoutePolyline(double startLat, double startLng, double endLat, double endLng) {
        List<Point> points = new ArrayList<>();
        points.add(new Point(startLat, startLng));
        points.add(new Point(endLat, endLng));
        return encodePolyline(points);
    }

    public static class Point {
        public double lat;
        public double lng;
        public Point(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    private String encodePolyline(List<Point> points) {
        StringBuilder encoded = new StringBuilder();
        int lastLat = 0;
        int lastLng = 0;

        for (Point p : points) {
            int lat = (int) Math.round(p.lat * 1e5);
            int lng = (int) Math.round(p.lng * 1e5);

            int dLat = lat - lastLat;
            int dLng = lng - lastLng;

            encodeValue(dLat, encoded);
            encodeValue(dLng, encoded);

            lastLat = lat;
            lastLng = lng;
        }
        return encoded.toString();
    }

    private void encodeValue(int value, StringBuilder encoded) {
        int sVal = value < 0 ? ~(value << 1) : (value << 1);
        while (sVal >= 0x20) {
            int rem = (sVal & 0x1f) | 0x20;
            encoded.append((char) (rem + 63));
            sVal >>= 5;
        }
        encoded.append((char) (sVal + 63));
    }
}
