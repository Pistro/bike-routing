package routing.algorithms.exact;

import routing.graph.Edge;
import routing.graph.FullEdge;
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
    private LinkedList <FullEdge> newEdges = new LinkedList<>();
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
                    FullEdge ei = (FullEdge) current.getInEdges().get(0);
                    FullEdge eo = (FullEdge) current.getOutEdges().get(0);
                    if (ei != eo) {
                        FullEdge e_new = FullEdge.join(ei, eo);
                        newEdges.add(e_new);
                        ei.decouple();
                        eo.decouple();
                        g.getNodes().remove(current.getId());
                        if (!loops && e_new.getStart() == e_new.getStop() && scheduled.add(e_new.getStart())) {
                            toDo.add(e_new.getStart());
                        }
                    }
                } else if (current.getInEdges().size() == 2 && current.getOutEdges().size() == 2) {
                    FullEdge ei1 = (FullEdge) current.getInEdges().get(0);
                    Node ni1 = ei1.getStart();
                    FullEdge ei2 = (FullEdge) current.getInEdges().get(1);
                    Node ni2 = ei2.getStart();
                    FullEdge eo1 = (FullEdge) current.getOutEdges().get(0);
                    Node no1 = eo1.getStop();
                    FullEdge eo2 = (FullEdge) current.getOutEdges().get(1);
                    Node no2 = eo2.getStop();
                    // Check whether simple contraction is possible
                    if (((ni1 == no1 && ni2 == no2) || (ni1 == no2 && ni2 == no1)) && ni1 != current && ni2 != current) {
                        FullEdge en1, en2;
                        if (ni1 == no1 && ni2 == no2) {
                            en1 = FullEdge.join(ei1, eo2);
                            if (ei1.getId()==eo1.getId() && eo2.getId()==ei2.getId()) en2 = FullEdge.join(en1.getId(), ei2, eo1);
                            else en2 = FullEdge.join(en1.getId(), ei2, eo1);
                        } else {
                            en1 = FullEdge.join(ei1, eo1);
                            if (ei1.getId()==eo2.getId() && eo1.getId()==ei2.getId()) en2 = FullEdge.join(en1.getId(), ei2, eo2);
                            else en2 = FullEdge.join(ei2, eo2);
                        }
                        newEdges.add(en1);
                        newEdges.add(en2);
                        ei1.decouple();
                        ei2.decouple();
                        eo1.decouple();
                        eo2.decouple();
                        g.getNodes().remove(current.getId());
                    }
                }
            }
        }
        for (Iterator<FullEdge> it = newEdges.listIterator(); it.hasNext(); ) {
            Edge current = it.next();
            if (current.getStart() == null) it.remove();
        }
    }
    public LinkedList<FullEdge> getNewEdges() {
        return newEdges;
    }
}
