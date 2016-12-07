package routing.graph;

import routing.algorithms.heuristics.DistanceCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created by piete on 29/11/2015.
 */
public class Graph {
    protected HashMap<Long, Node> nodes = new HashMap<Long, Node>();

    public boolean hasNode(Long id) {
        return nodes.containsKey(id);
    }

    public Node addNode(long id, double lat, double lon) {
        Node out = nodes.get(id);
        if (out == null) {
            out = new SimpleNode(id, lat, lon);
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

    public int getOrder() {
        return nodes.size();
    }

    public int getSize() {
        int out = 0;
        for (Map.Entry<Long, Node> en : nodes.entrySet()) {
            out += en.getValue().getInEdges().size();
        }
        return out;
    }

    public HashMap<Long, Node> getNodes() {
        return nodes;
    }

    public Graph getSubgraph(Node start, double len) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Forward Dijkstra
        double curLen = -1;
        HashMap<Node, Double> forwardLens = new HashMap<>();
        Queue<NodeLen> q = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        q.add(new NodeLen(start, 0));
        while (curLen<len && !q.isEmpty()) {
            NodeLen curNl = q.poll();
            Node curNode = curNl.n;
            curLen = curNl.l;
            if (!forwardLens.containsKey(curNode)) {
                forwardLens.put(curNode, curLen);
                for (Edge e: curNode.getOutEdges()) {
                    double eTourLen = curLen+e.getLength()+dc.getDistance(e.getStop(), start);
                    if (!forwardLens.containsKey(e.getStop()) && eTourLen<len) {
                        q.add(new NodeLen(e.getStop(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Backward Dijkstra
        curLen = -1;
        HashMap<Node, Double> backwardLens = new HashMap<>();
        q.clear();
        q.add(new NodeLen(start, 0));
        while (curLen<len && !q.isEmpty()) {
            NodeLen curNl = q.poll();
            Node curNode = curNl.n;
            curLen = curNl.l;
            if (!backwardLens.containsKey(curNode)) {
                backwardLens.put(curNode, curLen);
                for (Edge e: curNode.getInEdges()) {
                    double eTourLen = curLen+e.getLength()+dc.getDistance(start, e.getStart());
                    if (!backwardLens.containsKey(e.getStart()) && eTourLen<len) {
                        q.add(new NodeLen(e.getStart(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Extract subgraph
        Graph out = new Graph();
        for (Map.Entry<Node, Double> nd: forwardLens.entrySet()) {
            Double backwardLen = backwardLens.get(nd.getKey());
            if (backwardLen!=null && nd.getValue()+backwardLen<=len) {
                out.addNode(new SimpleNode((SimpleNode) nd.getKey()));
            }
        }
        for (Node n: forwardLens.keySet()) {
            Node n0 = out.getNode(n.getId());
            if (n0!=null) {
                for (Edge e: n.getOutEdges()) {
                    Node n1 = out.getNode(e.getStop().getId());
                    if (n1!=null) {
                        Edge e_new = new Edge(e, n0, n1);
                        e_new.id = e.id;
                    }
                }
            }
        }
        return out;
    }

    private class NodeLen {
        private Node n;
        private double l;
        public NodeLen(Node n, double l) {
            this.n = n;
            this.l = l;
        }
    }
}
