package routing.algorithms.exact;

import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;

import java.util.*;

/**
 * Tarjans algorithm
 * Created by pieter on 27/02/2016.
 */
public class ComponentDiscovery {
    private Stack<Node> s = new Stack<>();
    private HashMap<Node, AlgInfo> extraNodeInfo = new HashMap<>();
    private LinkedList<Set<Node>> components = new LinkedList<>();

    private static class AlgInfo {
        private static int nrIndices = 0;
        private int index;
        private int lowlink;
        private boolean onStack;
        private AlgInfo() {
            index = nrIndices;
            nrIndices++;
            lowlink = index;
            onStack = true;
        }
    }
    public ComponentDiscovery(Graph g) {
        for(Node n : g.getNodes().values()) {
            if (!extraNodeInfo.containsKey(n)) {
                strongConnect(n);
            }
        }
        extraNodeInfo.clear();
    }
    public LinkedList<Set<Node>> getComponents() {
        return components;
    }
    private void strongConnect(Node v) {
        AlgInfo vInfo = new AlgInfo();
        extraNodeInfo.put(v, vInfo);
        s.push(v);
        for (Edge e : v.getOutEdges()) {
            Node w = e.getStop();
            if (!extraNodeInfo.containsKey(w)) {
                // Successor w has not yet been visited; recurse on it
                strongConnect(w);
                vInfo.lowlink = Math.min(vInfo.lowlink, extraNodeInfo.get(w).lowlink);
            } else {
                AlgInfo wInfo = extraNodeInfo.get(w);
                if (wInfo.onStack) {
                    // Successor w is in stack S and hence in the current SCC
                    vInfo.lowlink = Math.min(vInfo.lowlink, wInfo.index);
                }
            }
        }
        // If v is a root node, pop the stack and generate an SCC
        if (vInfo.lowlink == vInfo.index) {
            // Start a new strongly connected component
            Set<Node> component = new HashSet<>();
            Node w;
            do {
                w = s.pop();
                extraNodeInfo.get(w).onStack = false;
                // Add w to current strongly connected component
                component.add(w);
            } while (w!=v);
            components.add(component);
        }
    }
}
