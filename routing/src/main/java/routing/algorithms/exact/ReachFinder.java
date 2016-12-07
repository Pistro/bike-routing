package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.HashMap;

/**
 * Find reaches using all-pairs shortest path algoritms
 * Created by pieter on 29/02/2016.
 */
public class ReachFinder extends FloydWarshall {
    private double [] [] mapDist;

    public class ReachValues {
        public double weight;
        public double length;
        public double dist;
        public ReachValues(double weight, double length, double dist) {
            this.weight = weight;
            this.length = length;
            this.dist = dist;
        }
    }

    private HashMap<Node, ReachValues> reaches = new HashMap<Node, ReachValues>();
    public ReachFinder(Graph g, WeightGetter wg) {
        super(g, wg);
        HashMap<Long, Node> nodes = g.getNodes();
        int nrNodes = nodes.size();
        mapDist = new double[nrNodes][nrNodes];
        for (int i = 0; i<nrNodes; i++) {
            Node u = reverseMapping[i];
            for (int j = i; j<nrNodes; j++) {
                Node v = reverseMapping[j];
                mapDist[i][j] = u.getDistance(v);
                mapDist[j][i] = mapDist[i][j];
            }
        }
        for (int k = 0; k<nrNodes; k++) {
            //if ((100*k/nrNodes)>(100*(k-1)/nrNodes)) System.out.println((100*k/nrNodes) + "%");
            double maxWeight = 0;
            double maxLength = 0;
            double maxDist = 0;
            for (int i = 0; i<nrNodes; i++) {
                if (lengths[i][k]>maxLength || distances[i][k]>maxWeight) {
                    for (int j = 0; j<nrNodes; j++) {
                        if (Math.abs(distances[i][k] + distances[k][j] - distances[i][j])<0.01) {
                            // k is on the shortest path between i and j
                            maxWeight = Math.max(maxWeight, Math.min(distances[i][k], distances[k][j]));
                            maxLength = Math.max(maxLength, Math.min(lengths[i][k], lengths[k][j]));
                            maxDist = Math.max(maxDist, Math.min(mapDist[i][k], mapDist[k][j]));
                            /*if (k == mapping.get(g.getNode(33503567)) && Math.abs(Math.min(lengths[i][k], lengths[k][j])-344.91)<0.1) {
                                System.out.println("Got it!");
                                Node start = reverseMapping[i], inter = reverseMapping[k], stop = reverseMapping[j];
                                System.out.println(start.getId() + " --> " + inter.getId() + " w:" + getPath(start, inter).getWeight(g) + " l:" + getPath(start, inter).getLength());
                                System.out.println(inter.getId() + " --> " + stop.getId() + " w:" + getPath(inter, stop).getWeight(g) + " l:" + getPath(inter, stop).getLength());
                                System.out.println(start.getId() + " --> " + stop.getId() + " w:" + getPath(start, stop).getWeight(g) + " l:" + getPath(start, stop).getLength());
                                System.out.println(getPath(start, stop));
                            }*/
                        }
                    }
                }
            }
            reaches.put(reverseMapping[k], new ReachValues(maxWeight, maxLength, maxDist));
        }
    }
    public HashMap<Node, ReachValues> getReaches() {
        return reaches;
    }
}
