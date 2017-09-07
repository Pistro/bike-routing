/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.graph;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.algorithms.heuristics.DistanceCalculator;

import java.util.*;

/**
 *
 * @author Pieter
 */
public class Path {
    private static double epsilon = 0.00001;
    private Node start;
    private final LinkedList<Edge> edges;

    public Path(Node start, LinkedList<Edge> edges) {
        this.start = start;
        this.edges = edges;
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
    public String toString() {
        String s = Long.toString(start.getId());
        for (Edge e : edges) s += " -" + e.getId() + "-> " + e.getStop().getId();
        return s;
    }

    public double getInterference(double strictness, DistanceCalculator dc) {
        double l = getLength();
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