package routing.graph;

import org.json.simple.JSONArray;

/**
 * This class represents a directed graph-edge.
 * Created by Pieter on 18/04/2016.
 */
public class Edge {
    private static int idCnt;
    private final int id;
    private Node start;
    private Node stop;
    private final double length;
    private final double wFast;
    private final double wAttr;
    private final double wSafe;
    public Node [] intermediateNodes = null;

    public Edge(int id, Node start, Node stop, double length, double wFast, double wAttr, double wSafe) {
        this.id = id;
        idCnt = Math.max(idCnt, id+1);
        this.start = start;
        this.stop = stop;
        this.length = length;
        this.wFast = wFast;
        this.wAttr = wAttr;
        this.wSafe = wSafe;
        couple();
    }

    public Edge(Edge e, Node start, Node stop) {
        this(e.getId(), start, stop, e.getLength(), e.getWFast(), e.getWAttr(), e.getWSafe());
        intermediateNodes = e.intermediateNodes;
    }

    public Edge(Edge e) {
        this(e.getId(), e.start, e.stop, e.getLength(), e.getWFast(), e.getWAttr(), e.getWSafe());
        intermediateNodes = e.intermediateNodes;
    }

    public void couple() {
        if (start!=null) start.addOutEdge(this);
        if (stop!=null) stop.addInEdge(this);
    }
    public void decouple() {
        start.getOutEdges().remove(this);
        stop.getInEdges().remove(this);
        start = null;
        stop = null;
    }

    public int getId() { return id; }
    public Node getStart() { return start; }
    public Node getStop() { return stop; }
    public void setStart(Node start) { this.start = start; }
    public void setStop(Node stop) { this.stop = stop; }
    public double getLength() { return length; }
    public double getWFast() { return wFast; }
    public double getWAttr() { return wAttr; }
    public double getWSafe() { return wSafe; }

    public String toString() { return Integer.toString(id); }

    public static Edge join(Edge first, Edge last) {
        Edge out = new Edge(idCnt, first.start, last.stop, first.length+last.length, first.wFast+last.wFast, first.wAttr+last.wAttr, first.wSafe+last.wSafe);
        out.intermediateNodes = new Node[first.intermediateNodes.length+last.intermediateNodes.length+1];
        System.arraycopy(first.intermediateNodes, 0, out.intermediateNodes, 0, first.intermediateNodes.length);
        out.intermediateNodes[first.intermediateNodes.length] = first.stop;
        System.arraycopy(last.intermediateNodes, 0, out.intermediateNodes, first.intermediateNodes.length+1, last.intermediateNodes.length);
        return out;
    }

    public static Edge getUncoupledEdge(Edge e, Node start, Node stop) {
        Edge out = new Edge(e, null, null);
        out.start = start;
        out.stop = stop;
        return out;
    }

    public static Edge getUncoupledEdge(Edge e) {
        Edge out = new Edge(e, null, null);
        out.start = e.start;
        out.stop = e.stop;
        return out;
    }

    public JSONArray toJSON() {
        JSONArray out = new JSONArray();
        out.add(start.toJSON());
        for (Node n: intermediateNodes) out.add(n.toJSON());
        out.add(stop.toJSON());
        return out;
    }
}
