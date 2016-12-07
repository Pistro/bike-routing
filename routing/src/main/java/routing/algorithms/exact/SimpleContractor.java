package routing.algorithms.exact;

import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by pieter on 28/02/2016.
 */
public class SimpleContractor {
    private boolean loops = true;
    private Set<Node> nonContractable = new HashSet<>();
    private LinkedList <Edge> newEdges = new LinkedList<Edge>();
    public SimpleContractor(Graph g, boolean loops, Set<Node> nonContractable) {
        this.loops = loops;
        this.nonContractable = nonContractable;
        contract(g);
    }
    public SimpleContractor(Graph g) {
        contract(g);
    }

    private void contract(Graph g) {
        HashSet<Node> scheduled = new HashSet<> (g.getNodes().values());
        LinkedList<Node> toDo = new LinkedList<>(g.getNodes().values());
        while (!toDo.isEmpty()) {
            Node current = toDo.pop();
            scheduled.remove(current);
            if (!loops) {
                for (Edge e: new LinkedList<>(current.getOutEdges())) {
                    if (e.getStart()==e.getStop()) e.decouple();
                }
            }
            if (!nonContractable.contains(current)) {
                if (current.getInEdges().size() == 1 && current.getOutEdges().size() == 1) {
                    Edge ei = current.getInEdges().get(0);
                    Edge eo = current.getOutEdges().get(0);
                    if (ei != eo) {
                        Edge e_new = Edge.join(ei, eo);
                        newEdges.add(e_new);
                        ei.decouple();
                        eo.decouple();
                        g.getNodes().remove(current.getId());
                        if (!loops && e_new.getStart() == e_new.getStop() && scheduled.add(e_new.getStart())) {
                            toDo.add(e_new.getStart());
                        }
                    }
                } else if (current.getInEdges().size() == 2 && current.getOutEdges().size() == 2) {
                    Edge ei1 = current.getInEdges().get(0);
                    Node ni1 = ei1.getStart();
                    Edge ei2 = current.getInEdges().get(1);
                    Node ni2 = ei2.getStart();
                    Edge eo1 = current.getOutEdges().get(0);
                    Node no1 = eo1.getStop();
                    Edge eo2 = current.getOutEdges().get(1);
                    Node no2 = eo2.getStop();
                    // Check whether simple contraction is possible
                    if (((ni1 == no1 && ni2 == no2) || (ni1 == no2 && ni2 == no1)) && ni1 != current && ni2 != current) {
                        if (ni1 == no1 && ni2 == no2) {
                            newEdges.add(Edge.join(ei1, eo2));
                            newEdges.add(Edge.join(ei2, eo1));
                        } else {
                            newEdges.add(Edge.join(ei1, eo1));
                            newEdges.add(Edge.join(ei2, eo2));
                        }
                        ei1.decouple();
                        ei2.decouple();
                        eo1.decouple();
                        eo2.decouple();
                        g.getNodes().remove(current.getId());
                    }
                }
            }
        }
        for (Iterator<Edge> it = newEdges.listIterator(); it.hasNext(); ) {
            Edge current = it.next();
            if (current.getStart() == null) it.remove();
        }
    }
    public LinkedList<Edge> getNewEdges() {
        return newEdges;
    }
}
