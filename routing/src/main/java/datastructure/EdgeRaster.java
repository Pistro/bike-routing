package datastructure;

import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.Edge;
import routing.graph.SimpleNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pieter on 30/11/2016.
 */
public class EdgeRaster {
    private final double rasterSize;
    private final DistanceCalculator dc;
    private final HashMap<IntPair, HashSet<Edge>> tiles = new HashMap<>();
    public EdgeRaster(Collection<Edge> edges, double rasterSize) {
        this.rasterSize = rasterSize;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (Edge e: edges) {
            maxLat = Math.max(maxLat, e.getStart().getLat());
            minLat = Math.min(minLat, e.getStart().getLat());
            maxLon = Math.max(maxLon, e.getStop().getLon());
            minLon = Math.min(minLon, e.getStop().getLon());
        }
        dc = new DistanceCalculator(new SimpleNode((maxLat+minLat)/2, (maxLon+minLon)/2));
        for (Edge e: edges) addEdge(e);
    }
    public void addEdge(Edge e) {
        double latAvg = (e.getStart().getLat()+e.getStop().getLat())/2;
        double lonAvg = (e.getStart().getLon()+e.getStop().getLon())/2;
        double rad = 1/Math.PI*(e.getLength()+dc.getDistance(e.getStart(), e.getStop()));
        int latStart = (int) ((latAvg*dc.getLatScale()-rad)/rasterSize);
        int latMid = (int) (latAvg*dc.getLatScale()/rasterSize);
        int latStop = (int) ((latAvg*dc.getLatScale()+rad)/rasterSize);
        int lonStart = (int) ((lonAvg*dc.getLonScale()-rad)/rasterSize);
        int lonMid = (int) (lonAvg*dc.getLonScale()/rasterSize);
        int lonStop = (int) ((lonAvg*dc.getLonScale()+rad)/rasterSize);
        for (int i=latStart; i<=latStop; i++) {
            double latSq = (i<latMid)? (i+1)*rasterSize/dc.getLatScale() : (i==latMid)? latAvg : (i-1)*rasterSize/dc.getLatScale();
            for (int j=lonStart; j<=lonStop; j++) {
                double lonSq = (j<lonMid)? (j+1)*rasterSize/dc.getLonScale() : (j==lonMid)? lonAvg : (j-1)*rasterSize/dc.getLonScale();
                if (dc.getDistance(latAvg, lonAvg, latSq, lonSq)<=rad) {
                    HashSet<Edge> curEdges = tiles.computeIfAbsent(new IntPair(i, j), k -> new HashSet<>());
                    curEdges.add(e);
                }
            }
        }
    }
    public HashSet<IntPair> getNeighbourTiles(Edge e, double range) {
        HashSet<IntPair> tileKeys = new HashSet<>();
        double latAvg = (e.getStart().getLat()+e.getStop().getLat())/2;
        double lonAvg = (e.getStart().getLon()+e.getStop().getLon())/2;
        int latStart = (int) ((latAvg*dc.getLatScale()-range)/rasterSize);
        int latMid = (int) (latAvg*dc.getLatScale()/rasterSize);
        int latStop = (int) ((latAvg*dc.getLatScale()+range)/rasterSize);
        int lonStart = (int) ((lonAvg*dc.getLonScale()-range)/rasterSize);
        int lonMid = (int) (lonAvg*dc.getLonScale()/rasterSize);
        int lonStop = (int) ((lonAvg*dc.getLonScale()+range)/rasterSize);
        for (int i=latStart; i<=latStop; i++) {
            double latSq = (i<latMid)? (i+1)*rasterSize/dc.getLatScale() : (i==latMid)? latAvg : (i-1)*rasterSize/dc.getLatScale();
            for (int j=lonStart; j<=lonStop; j++) {
                double lonSq = (j<lonMid)? (j+1)*rasterSize/dc.getLonScale() : (j==lonMid)? lonAvg : (j-1)*rasterSize/dc.getLonScale();
                if (dc.getDistance(latAvg, lonAvg, latSq, lonSq)<=range) {
                    tileKeys.add(new IntPair(i, j));
                }
            }
        }
        return tileKeys;
    }
    public HashSet<Edge> collectTiles(Collection<IntPair> pairs) {
        HashSet<Edge> out = new HashSet<>();
        for (IntPair ip: pairs) {
            HashSet<Edge> ipEdges = tiles.get(ip);
            if (ipEdges!=null) out.addAll(ipEdges);
        }
        return out;
    }
}
