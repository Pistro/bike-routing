/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.graph;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.LinkedList;

/**
 *
 * @author Pieter
 */
public class Node {
    private static long idCnt;
    private final Long id;
    private final LinkedList<Edge> outEdges = new LinkedList<Edge>();
    private final LinkedList<Edge> inEdges = new LinkedList<Edge>();
    private final double lat;
    private final double lon;

    public void addOutEdge(Edge e) {
        outEdges.add(e);
    }
    public void addInEdge(Edge e) {
        inEdges.add(e);
    }
    public LinkedList<Edge> getOutEdges() {
        return outEdges;
    }
    public LinkedList<Edge> getInEdges() {
        return inEdges;
    }

    public Node(Long id, double lat, double lon) {
        this.id = id;
        idCnt = Math.max(idCnt, id+1);
        this.lat = lat;
        this.lon = lon;
    }
    public Node(double lat, double lon) {
        this.id = idCnt++;
        this.lat = lat;
        this.lon = lon;
    }

    public Long getId() { return id; }
    public double getLat() {return lat; };
    public double getLon() { return lon; };

    public double getDistance(Node n) {
        return distFrom(getLat(), getLon(), n.getLat(), n.getLon());
    }

    public String toString() { return id.toString(); }

    // Code from: http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
    private static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return earthRadius * c;
    }
}
