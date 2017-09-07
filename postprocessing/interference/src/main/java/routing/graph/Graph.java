package routing.graph;

import routing.algorithms.heuristics.DistanceCalculator;

import java.util.*;

/**
 * Created by Pieter on 29/11/2015.
 */
public class Graph {
    protected HashMap<Long, Node> nodes = new HashMap<Long, Node>();

    public Node addNode(long id, double lat, double lon) {
        Node out = nodes.get(id);
        if (out == null) {
            out = new Node(id, lat, lon);
            nodes.put(id, out);
        }
        return out;
    }

    public Node addNode(Node n) {
        return nodes.put(n.getId(), n);
    }

    public Node getNode(Long id) {
        return nodes.get(id);
    }

     public HashMap<Long, Node> getNodes() {
        return nodes;
    }

    public LinkedList<Edge> getEdges() {
        LinkedList<Edge> out = new LinkedList<>();
        for (Node n: nodes.values()) out.addAll(n.getOutEdges());
        return out;
    }
}
