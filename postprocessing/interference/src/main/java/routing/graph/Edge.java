package routing.graph;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class represents a directed graph-edge.
 * Created by Pieter on 18/04/2016.
 */
public class Edge {
    private static int idCnt;
    private int id;
    private Node start;
    private Node stop;
    private final double length;
    public Long [] shadow = null;
    public Edge(int id, Node start, Node stop, double length) {
        this.id = id;
        idCnt = Math.max(idCnt, id+1);
        this.start = start;
        this.stop = stop;
        this.length = length;
    }

    public void couple() {
        start.addOutEdge(this);
        stop.addInEdge(this);
    }
    public double getLength() { return length; }
    public int getId() { return id; }
    public Node getStart() { return start; }
    public Node getStop() { return stop; }

    public String toString() { return Integer.toString(id); }

}
