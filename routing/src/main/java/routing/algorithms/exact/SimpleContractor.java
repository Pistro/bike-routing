package routing.algorithms.exact;

import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Remove in-between nodes by joining neighbouring edges
 * Created by Pieter on 28/02/2016.
 */
public class SimpleContractor {
    public boolean loops = true;
    private LinkedList <Edge> newEdges = new LinkedList<>();
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
            if (current.getInEdges().size() == 1 && current.getOutEdges().size() == 1) {
                Edge ei = current.getInEdges().get(0);
                Edge eo = current.getOutEdges().get(0);
                if (ei != eo) {
                    Edge e_new = Edge.join(ei, eo);
                    newEdges.add(e_new);
                    ei.decouple();
                    eo.decouple();
                    g.getNodes().remove(current.getId());
                    if (scheduled.add(e_new.getStart())) toDo.add(e_new.getStart());
                    if (scheduled.add(e_new.getStop())) toDo.add(e_new.getStop());
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
                    Edge en1, en2;
                    if (ni1 == no1) {
                        en1 = Edge.join(ei1, eo2);
                        if (ei1.getId()==eo1.getId() && eo2.getId()==ei2.getId()) en2 = Edge.join(ei2, eo1);
                        else en2 = Edge.join(ei2, eo1);
                    } else {
                        en1 = Edge.join(ei1, eo1);
                        if (ei1.getId()==eo2.getId() && eo1.getId()==ei2.getId()) en2 = Edge.join(ei2, eo2);
                        else en2 = Edge.join(ei2, eo2);
                    }
                    newEdges.add(en1);
                    newEdges.add(en2);
                    ei1.decouple();
                    ei2.decouple();
                    eo1.decouple();
                    eo2.decouple();
                    g.getNodes().remove(current.getId());
                    if (scheduled.add(en1.getStart())) toDo.add(en1.getStart());
                    if (scheduled.add(en2.getStop())) toDo.add(en2.getStop());
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
