package routing.graph;

import java.util.HashMap;

/**
 * This class represents a directed graph-edge.
 * Created by Pieter on 18/04/2016.
 */
public abstract class Edge {
    private static int idCnt;
    private final int id;
    private Node start;
    private Node stop;
    private final double length;
    public int [] shadow = null;
    public Edge(int id, Node start, Node stop, double length) {
        this.id = id;
        idCnt = Math.max(idCnt, id+1);
        this.start = start;
        this.stop = stop;
        this.length = length;
    }
    public Edge(Edge e) {
        this(e.id, e.getStart(), e.getStop(), e.length);
    }
    public void couple() {
        start.addOutEdge(this);
        stop.addInEdge(this);
    }
    public void decouple() {
        start.getOutEdges().remove(this);
        stop.getInEdges().remove(this);
        start = null;
        stop = null;
    }
    public HashMap<String, String> getAttrs() {
        return new HashMap<>();
    }
    public double getLength() { return length; }
    public int getId() { return id; }
    public Node getStart() { return start; }
    public Node getStop() { return stop; }

    public String toString() { return Integer.toString(id); }

    public abstract double getWFast();
    public abstract double getWAttr();
    public abstract double getWSafe();
    public static int getFreeId() { return idCnt++; }
}
