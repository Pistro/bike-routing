/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.graph;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.weights.PoisonedWeightGetter;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 *
 * @author Pieter
 */
public class Path {
    private static double epsilon = 0.00001;
    private Node start;
    private final LinkedList<Edge> edges;
    private final HashMap<String, Object> tags;
    private final LinkedList<Node> markPoints;

    public Path(Node start, LinkedList<Edge> edges) {
        this.start = start;
        this.edges = edges;
        this.tags = new HashMap<>();
        this.markPoints = new LinkedList<>();
    }
    public Path(Node start) {
        this.start = start;
        this.edges = new LinkedList<>();
        this.tags = new HashMap<>();
        this.markPoints = new LinkedList<>();
    }
    public Path(Path p) {
        this.start = p.start;
        this.edges = new LinkedList<>(p.edges);
        this.tags = new HashMap<>(p.tags);
        this.markPoints = new LinkedList<>(p.markPoints);
    }
    public void flipForward() {
        if (edges.isEmpty()) return;
        Collections.reverse(edges);
        start = edges.getFirst().getStart();
    }
    public void addMarkpoint(Node n) { markPoints.add(n); }
    public void trim(int i) {
        while (this.edges.size()>i) edges.removeLast();
    }
    public void trim(Node n) {
        while (edges.getLast().getStop()!=n) {
            edges.removeLast();
        }
    }
    public void addEdge(Edge e) {
        edges.add(e);
    }
    public Object addTag(String key, Object value) {
        return tags.put(key, value);
    }
    public Node getStart() {
        return start;
    }
    public Node getEnd() { if (edges.isEmpty()) return start; else return edges.getLast().getStop(); }
    public void setStart(Node start) {
        this.start = start;
    }
    public LinkedList<Edge> getEdges() {
        return edges;
    }
    public void addPath(Path p) {
        if (getEnd() != p.start)
            throw new IllegalArgumentException("Start of added path does not equals end of current path");
        edges.addAll(p.edges);
    }
    public double getLength() { return getLength(edges.size()); }
    public double getLength(int pos) {
        double out = 0;
        Iterator<Edge> it = edges.listIterator();
        for (int i = 0; i<pos; i++) {
            out += it.next().getLength();
        }
        return out;
    }
    public double getWSafe() {
        double out = 0;
        for (Edge e: edges) {
            out += e.getWSafe();
        }
        return out;
    }
    public double getWAttr() {
        double out = 0;
        for (Edge e: edges) {
            out += e.getWAttr();
        }
        return out;
    }
    public double getWFast() {
        double out = 0;
        for (Edge e: edges) {
            out += e.getWFast();
        }
        return out;
    }
    public double getWeight(WeightGetter g) { return getWeight(g, edges.size()); }
    public double getWeight(WeightGetter g, int pos) {
        double out = 0;
        Iterator<Edge> it = edges.listIterator();
        for (int i = 0; i<pos; i++) {
            out += g.getWeight(it.next());
        }
        return out;
    }
    public Node getNode(int pos) {
        if (pos == 0) return start;
        else return edges.get(pos-1).getStop();
    }
    public boolean isTour() {
        return start == edges.getLast().getStop();
    }
    public JSONObject toJSON() {
        JSONObject route = new JSONObject();
        if (!tags.isEmpty()) {
            JSONObject routeTags = new JSONObject();
            routeTags.putAll(tags);
            route.put("tags", routeTags);
        }
        JSONArray nodes = new JSONArray();
        nodes.add(start.toJSON());
        for (Edge e : getEdges()) {
            for (Node n: e.intermediateNodes) {
                nodes.add(n.toJSON());
            }
            nodes.add(e.getStop().toJSON());
        }
        route.put("nodes", nodes);
        if (!markPoints.isEmpty()) {
            JSONArray mPoints = new JSONArray();
            for (Node n : markPoints) {
                JSONObject point = new JSONObject();
                point.put("lat", n.getLat());
                point.put("lon", n.getLon());
                mPoints.add(point);
            }
            route.put("markpoints", mPoints);
        }
        return route;
    }
    public String toString() {
        String s = Long.toString(start.getId());
        for (Edge e : edges) s += " -" + e.getId() + "-> " + e.getStop().getId();
        return s;
    }

    public double getInterference(double strictness) {
        double l = getLength();
        // Scaling factors
        DistanceCalculator dc = new DistanceCalculator(start);
        // Calculate interference
        double interference = 0;
        double curPos = 0;
        Iterator<Edge> it = edges.iterator();
        while(it.hasNext()) {
            Edge curEdge = it.next();
            curPos += curEdge.getLength()/2;
            // Compare current edge with predecessors
            double compPos = 0;
            Iterator<Edge> it2 = edges.iterator();
            while(it2.hasNext()) {
                Edge compEdge = it2.next();
                compPos += compEdge.getLength()/2;
                if (compPos+epsilon>=curPos) break;
                double frac = Math.min(curPos-compPos, l-curPos+compPos);
                double dist2 = dc.getDistance2(curEdge, compEdge);
                double expectedDist = 2*frac*strictness/Math.PI;
                if (dist2<expectedDist*expectedDist) {
                    double dist = dc.getDistance(curEdge, compEdge);
                    interference += (expectedDist-dist)/expectedDist*curEdge.getLength()*compEdge.getLength();
                }
                compPos += compEdge.getLength()/2;
            }
            curPos += curEdge.getLength()/2;
        }
        return 2*interference/(l*l);
    }
}