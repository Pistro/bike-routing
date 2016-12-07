package routing.graph;

import java.util.HashMap;
import java.util.StringJoiner;

/**
 * This class represents a directed graph-edge.
 * Created by pieter on 18/04/2016.
 */
public class Edge {
    private static int idCnt = 0;
    public int id;
    private Node start;
    private Node stop;
    private double length;
    private double heightDif;
    private double wFast;
    private double wAttr;
    private double wSafe;
    public int [] shadow = null;
    public Edge() {
        id = idCnt++;
    }
    public Edge(Node start, Node stop, double length, double heightDif, double wFast, double wAttr, double wSafe) {
        id = idCnt++;
        this.start = start;
        this.stop = stop;
        this.length = length;
        this.heightDif = heightDif;
        this.wFast = wFast;
        this.wAttr = wAttr;
        this.wSafe = wSafe;
        start.addOutEdge(this);
        stop.addInEdge(this);
    }
    public Edge(Edge e, Node start, Node stop) {
        this(start, stop, e.length, e.heightDif, e.wFast, e.wAttr, e.wSafe);
        shadow = e.shadow;
    }
    public static Edge getUncoupledEdge(Edge e, Node start, Node stop) {
        Edge out = new Edge();
        out.start = start;
        out.stop = stop;
        out.length = e.length;
        out.heightDif = e.heightDif;
        out.wFast = e.wFast;
        out.wAttr = e.wAttr;
        out.wSafe = e.wSafe;
        out.shadow = e.shadow;
        return out;
    }
    public Edge getTwin() {
        for (Edge e: stop.getOutEdges()) {
            if (e.getStop() == start) {
                if (shadow.length == e.shadow.length) {
                    boolean equal = true;
                    for (int i = 0; i<shadow.length; i++) {
                        if (shadow[i]!=e.shadow[e.shadow.length-1-i]) {
                            equal = false;
                            break;
                        }
                    }
                    if (equal) return e;
                }
            }
        }
        return null;
    }
    public static Edge join(Edge start, Edge stop) {
        if (start.stop != stop.start) throw new IllegalArgumentException("Attached edge should start where the attaching edge ends");
        Edge out = new Edge(start.start, stop.stop, Math.round((start.length+stop.length)*100)/100.0, Math.round((start.heightDif+stop.heightDif)*100)/100.0,
                Math.round((start.wFast+stop.wFast)*100)/100.0, Math.round((start.wAttr+stop.wAttr)*100)/100.0, Math.round((start.wSafe+stop.wSafe)*100)/100.0);
        out.shadow = new int[start.shadow.length+stop.shadow.length];
        System.arraycopy(start.shadow, 0, out.shadow, 0, start.shadow.length);
        System.arraycopy(stop.shadow, 0, out.shadow, start.shadow.length, stop.shadow.length);
        return out;
    }
    public void decouple() {
        start.getOutEdges().remove(this);
        stop.getInEdges().remove(this);
        start = null;
        stop = null;
    }
    public HashMap<String, String> getAttrs() {
        HashMap<String, String> out = new HashMap<String, String>();
        return out;
    }
    public HashMap<String, String> getTags() {
        HashMap<String, String> out = new HashMap<String, String>();
        out.put("start_node", Long.toString(start.getId()));
        out.put("end_node", Long.toString(stop.getId()));
        out.put("score_safe", Double.toString(wSafe));
        out.put("score_attr", Double.toString(wAttr));
        out.put("score_fast", Double.toString(wFast));
        out.put("height_dif", Double.toString(heightDif));
        out.put("length", Double.toString(length));
        return out;
    }
    public double getLength() { return length; }
    public double getHeightDif() { return heightDif; }
    public double getWFast() { return wFast; }
    public double getWAttr() { return wAttr; }
    public double getWSafe() { return wSafe; }
    public Node getStart() { return start; }
    public Node getStop() { return stop; }
    public void scale(double newLength) {
        if (length<=0) {
            if (wFast>0) length = wFast;
            else {
                wFast = 1;
                length = 1;
            }
        }
        double factor = newLength/length;
        heightDif *= factor;
        wFast *= factor;
        wAttr *= factor;
        wSafe *= factor;
        length = newLength;
    }
    public void setLength(double length) { this.length = length; }
    public void setHeightDif(double hd) { this.heightDif = hd; }
    public void setWFast(double wFast) { this.wFast = wFast; }
    public void setWAttr(double wAttr) { this.wAttr = wAttr; }
    public void setWSafe(double wSafe) { this.wSafe = wSafe; }
    public void setStart(Node start) { this.start = start; }
    public void setStop(Node end) { this.stop = end; }

    public String toString() { return Integer.toString(id); }
}
