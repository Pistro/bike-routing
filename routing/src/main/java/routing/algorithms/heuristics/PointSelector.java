package routing.algorithms.heuristics;

import routing.graph.Graph;
import routing.graph.Node;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Selects a specified amount of nodes, aiming for the most uniform geographical distribution
 * Created by Pieter on 22/11/2016.
 */
public class PointSelector {
    private final Graph g;
    private final int nr;
    public final LinkedList<Node> selected = new LinkedList<>();
    private final int attempts;

    public PointSelector(Graph g, int nr, int attempts) {
        this.g = g;
        this.nr = nr;
        this.attempts = attempts;
    }

    public void execute() {
        ArrayList<Node> nodes = new ArrayList<>(g.getNodes().values());
        while (selected.size()<nr) {
            Node maxDistNode = null;
            double maxDist = -1;
            for (int i = 0; i<attempts; i++) {
                Node sel = nodes.get((int)(Math.random()*nodes.size()));
                double selDist = Double.MAX_VALUE;
                for (Node n: selected) selDist = Math.min(selDist, n.getDistance(sel));
                if (selDist>maxDist) {
                    maxDist = selDist;
                    maxDistNode = sel;
                }
            }
            selected.add(maxDistNode);
        }
    }

}
