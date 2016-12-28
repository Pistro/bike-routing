package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * All-pairs shortest path
 * Created by piete on 28/02/2016.
 */
public class FloydWarshall {
    private HashMap<Node, Integer> mapping = new HashMap<>();
    protected Node[] reverseMapping;
    protected double [][] lengths;
    protected double [][] distances;
    private Edge [][] next;
    public double getDistance(Node start, Node stop) {
        return distances[mapping.get(start)][mapping.get(stop)];
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
        distances = new double[nrNodes][nrNodes];
        lengths = new double[nrNodes][nrNodes];
        next = new Edge[nrNodes][nrNodes];
        for (int i = 0; i<nrNodes; i++) for (int j = 0; j<nrNodes; j++) distances[i][j] = Double.MAX_VALUE;
        int pos = 0;
        for (Node n : nodes.values()) {
            mapping.put(n, pos);
            reverseMapping[pos] = n;
            distances[pos][pos] = 0;
            pos++;
        }
        for (Node u: nodes.values()) {
            for (Edge e: u.getOutEdges()) {
                Node v = e.getStop();
                if (wg.getWeight(e)<distances[mapping.get(u)][mapping.get(v)]) {
                    distances[mapping.get(u)][mapping.get(v)] = wg.getWeight(e);
                    lengths[mapping.get(u)][mapping.get(v)] = e.getLength();
                    next[mapping.get(u)][mapping.get(v)] = e;
                }
            }
        }
        for (int k = 0; k<nrNodes; k++) {
            //if ((100*k/nrNodes)>((k-1)/nrNodes)) System.out.println((100*k/nrNodes) + "%");
            for (int i = 0; i<nrNodes; i++) {
                for (int j = 0; j<nrNodes; j++) {
                    if (distances[i][j] > distances[i][k] + distances[k][j]) {
                        distances[i][j] = distances[i][k] + distances[k][j];
                        lengths[i][j] = lengths[i][k] + lengths[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
    }
}
