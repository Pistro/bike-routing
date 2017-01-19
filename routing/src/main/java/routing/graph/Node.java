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
public abstract class Node {
    private static long idCnt;
    private final Long id;
    private final LinkedList<Edge> outEdges = new LinkedList<Edge>();
    private final LinkedList<Edge> inEdges = new LinkedList<Edge>();
    private double reach = -1;

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
    public boolean hasReach() { return reach!=-1; }
    public synchronized void setReach(double d) { if (d>reach) reach = d; }
    public double getReach() { return reach; }
    public void clearReach() { reach = -1; }

    public Node(long id) { this.id = id; idCnt = Math.max(idCnt, id+1); }
    public Node() { this.id = idCnt++; }

    public void decouple() {
        for (Edge e: new LinkedList<>(inEdges)) e.decouple();
        for (Edge e: new LinkedList<>(outEdges)) e.decouple();
    }

    public Long getId() { return id; }
    public abstract double getLat();
    public abstract double getLon();

    public JSONObject toJSON() {
        JSONObject out = new JSONObject();
        out.put("id", getId());
        out.put("lat", getLat());
        out.put("lon", getLon());
        return out;
    }

    public double getDistance(Node n) {
        return distFrom(getLat(), getLon(), n.getLat(), n.getLon());
    }

    // Code from: http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
    private static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }

    public abstract Node clone();
}
