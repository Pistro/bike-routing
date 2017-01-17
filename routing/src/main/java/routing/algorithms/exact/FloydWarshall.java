package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Floyd-Warshall algorithm to find all-pairs shortest paths
 * Created by Pieter on 28/02/2016.
 */
public class FloydWarshall {
    private HashMap<Node, Integer> mapping = new HashMap<>();
    protected Node[] reverseMapping;
    protected double [][] costs;
    protected double [][] lengths;
    private Edge [][] next;

    public double getDistance(Node start, Node stop) {
        return costs[mapping.get(start)][mapping.get(stop)];
    }

    public Path getPath(Node start, Node stop) {
        int stop_idx = mapping.get(stop);
        Node current = start;
        LinkedList<Edge> edges = new LinkedList<>();
        while (current !=null && current != stop) {
            Edge e = next[mapping.get(current)][stop_idx];
            edges.add(e);
            current = e.getStop();
        }
        if (current == null) return null;
        else return new Path(start, edges);
    }

    public FloydWarshall(Graph g, WeightGetter wg) {
        HashMap<Long, Node> nodes = g.getNodes();
        reverseMapping = new Node[nodes.size()];
        int nrNodes = nodes.size();
        costs = new double[nrNodes][nrNodes];
        lengths = new double[nrNodes][nrNodes];
        next = new Edge[nrNodes][nrNodes];
        for (int i = 0; i<nrNodes; i++) for (int j = 0; j<nrNodes; j++) {
            costs[i][j] = Double.MAX_VALUE;
            lengths[i][j] = Double.MAX_VALUE;
        }
        int pos = 0;
        for (Node n : nodes.values()) {
            mapping.put(n, pos);
            reverseMapping[pos] = n;
            costs[pos][pos] = 0;
            lengths[pos][pos] = 0;
            pos++;
        }
        for (Node u: nodes.values()) {
            int uIdx = mapping.get(u);
            for (Edge e: u.getOutEdges()) {
                Node v = e.getStop();
                int vIdx = mapping.get(v);
                if (wg.getWeight(e)<costs[uIdx][vIdx]) {
                    costs[uIdx][vIdx] = wg.getWeight(e);
                    lengths[uIdx][vIdx] = e.getLength();
                    next[uIdx][vIdx] = e;
                }
            }
        }
        for (int k = 0; k<nrNodes; k++) {
            for (int i = 0; i<nrNodes; i++) {
                for (int j = 0; j<nrNodes; j++) {
                    if (costs[i][j] > costs[i][k] + costs[k][j]) {
                        costs[i][j] = costs[i][k] + costs[k][j];
                        lengths[i][j] = lengths[i][k] + lengths[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
    }
}
