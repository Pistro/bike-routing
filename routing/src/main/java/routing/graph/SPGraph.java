package routing.graph;

import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.exact.Dijkstra;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.weights.WeightBalancer;
import routing.graph.weights.WeightGetter;
import routing.graph.weights.WeightLength;

import java.util.*;

/**
 * Creates a graph in which only paths are present that are least cost according to the nodes encountered a last number of meters.
 * Created by Pieter on 24/10/2016.
 */
public class SPGraph extends Graph {
    private double reach;
    private static final double epsilon = 0.00001;
    private WeightBalancer wg;
    private final GraphNodePairSet hyperNodes;
    private boolean bi;

    public double getReach() { return reach; }
    public void setReach(double reach) { this.reach = reach; }
    public WeightBalancer getWeightBalancer() { return wg; }
    public void setWeightBalancer(WeightBalancer wg) { this.wg = wg; }
    public void setBi(boolean bi) { this.bi = bi; }
    public boolean getBi() { return bi; }

    public NodePair getNodePair(Node start, Node stop) {
        return hyperNodes.getNodePair(start, stop);
    }

    private class GraphNodePairSet {
        private long idCnt = 0;
        private final Graph gr;
        private final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private GraphNodePairSet(Graph gr) {
            this.gr = gr;
        }

        private NodePair addNodePair(Node s, Node e) {
            return addNodePair(++idCnt, s, e);
        }

        private NodePair addNodePair(long id, Node s, Node e) {
            NodePair last = new NodePair(id, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                gr.addNode(last);
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }

        private NodePair getNodePair(Node s, Node e) {
            return pairs.get(new NodePair(0, s, e));
        }
        private NodePair getNodePair(NodePair np) {
            return pairs.get(np);
        }
    }

    public static class NodePair extends Node {
        public Node s;
        public Node e;

        public NodePair(long id, Node s, Node e) {
            super(id);
            this.s = s;
            this.e = e;
        }
        public double getLat() { return e.getLat(); }
        public double getLon() { return e.getLon(); }
        @Override
        public int hashCode() {
            long hash = 1;
            hash = hash * 17 + s.getId();
            hash = hash * 31 + e.getId();
            return (int) hash;
        }
        @Override
        public boolean equals(Object aThat) {
            if ( this == aThat ) return true;
            if ( !(aThat instanceof NodePair) ) return false;
            NodePair that = (NodePair)aThat;
            return s.getId().equals(that.s.getId()) && e.getId().equals(that.e.getId());
        }
        public NodePair clone() {
            return new NodePair(getId(), s, e);
        }
    }

    public Node addNodePair(long id, Node s, Node e) {
        return hyperNodes.addNodePair(id, s, e);
    }

    public SPGraph() { hyperNodes = new GraphNodePairSet(this); }

    public SPGraph(Graph g, double reach, boolean bidirectional, WeightBalancer wb) {
        this.reach = reach;
        this.wg = wb;
        this.bi = bidirectional;
        // Construct hypergraph using every possible starting node
        hyperNodes = new GraphNodePairSet(this);
        int i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            contract(n, (added, lastNode, lastEdge, pLen) -> {
                if (pLen > reach + epsilon && pLen - lastEdge.getLength() <= reach + epsilon) {
                    Set<Edge> edgeSet = new HashSet<>();
                    ArrayList<Edge> edgeList = new ArrayList<>();
                    edgeSet.add(lastEdge);
                    edgeList.add(lastEdge);
                    for (int j = 0; j<edgeList.size(); j++) {
                        for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                            if (edgeSet.add(e)) edgeList.add(e);
                        }
                    }
                    Collections.reverse(edgeList);
                    for (Edge e: edgeList) {
                        if (pLen-added.get(e.getStart()).l>reach+epsilon && pLen-added.get(e.getStop()).l<=reach+epsilon) {
                            hyperNodes.addNodePair(e.getStop(), lastEdge.getStop());
                        }
                    }
                }
            });
        }
        i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            contract(n, (added, lastNode, lastEdge, pLen) -> {
                if (lastEdge == null) return;
                NodePair np0 = hyperNodes.getNodePair(n, lastEdge.getStart());
                if (np0 == null) return;
                if (pLen<=reach+epsilon) {
                    NodePair np1 = hyperNodes.addNodePair(n, lastEdge.getStop());
                    new SimpleEdge(lastEdge, np0, np1);
                } else if (pLen-lastEdge.getLength()<=reach+epsilon) {
                    Set<Edge> edgeSet = new HashSet<>();
                    ArrayList<Edge> edgeList = new ArrayList<>();
                    edgeSet.add(lastEdge);
                    edgeList.add(lastEdge);
                    for (int j = 0; j<edgeList.size(); j++) {
                        for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                            if (edgeSet.add(e)) edgeList.add(e);
                        }
                    }
                    Collections.reverse(edgeList);
                    for (Edge e: edgeList) {
                        if (pLen-added.get(e.getStart()).l>reach+epsilon && pLen-added.get(e.getStop()).l<=reach+epsilon) {
                            NodePair np1 = hyperNodes.getNodePair(e.getStop(), lastNode);
                            new SimpleEdge(lastEdge, np0, np1);
                        }
                    }
                }
            });
        }
        if (!bidirectional) {
            // Resolve bidirectional edges
            for (Node cur : new LinkedList<>(getNodes().values())) {
                NodePair curNp = (NodePair) cur;
                for (Edge e : new LinkedList<>(cur.getOutEdges())) if (e.getStop() == cur) e.decouple();
                HashSet<Node> processedNbs = new HashSet<>();
                for (Edge e_in : new LinkedList<>(cur.getInEdges())) {
                    NodePair np_in = ((NodePair) e_in.getStart());
                    if (np_in!=null && processedNbs.add(np_in.e)) {
                        Node nb_in = np_in.e;
                        boolean split = false;
                        for (Edge e_out : cur.getOutEdges()) {
                            Node nb_out = ((NodePair) e_out.getStop()).e;
                            if (nb_in == nb_out) {
                                split = true;
                                break;
                            }
                        }
                        if (split) {
                            Node n_new = hyperNodes.addNodePair(nb_in, curNp.e);
                            if (n_new.getInEdges().size()==0) { // <-- Redundant check?
                                LinkedList<Edge> toDecouple = new LinkedList<>();
                                for (Edge e_in_alt : cur.getInEdges()) {
                                    if (((NodePair) e_in_alt.getStart()).e == nb_in) {
                                        new SimpleEdge(e_in_alt, e_in_alt.getStart(), n_new);
                                        toDecouple.add(e_in_alt);
                                    }
                                }
                                for (Edge e: toDecouple) e.decouple();
                                for (Edge e_out : cur.getOutEdges()) {
                                    if (((NodePair) e_out.getStop()).e != nb_in) {
                                        new SimpleEdge(e_out, n_new, e_out.getStop());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private interface TreeNodeProcessor {
        void process(HashMap<Node, CostLenEdgeSet> added, Node lastNode, Edge lastEdge, double pLen);
    }

    private void contract(Node n, TreeNodeProcessor p) {
        HashMap<Node, CostLenEdgeSet> added = new HashMap<>();
        HashMap<Node, CostLen> proposals = new HashMap<>();
        Queue<NodeEdgeCostLen> queue = new PriorityQueue<>((o1, o2) -> (o1.c<o2.c || (o1.c==o2.c && o1.c<o2.l))? -1 : ((o1.c>o2.c || (o1.c==o2.c && o1.l>o2.l))? 1 : 0));
        queue.add(new NodeEdgeCostLen(n, null, 0, 0));
        double lastCost = 0, lastLen = 0;
        while (!queue.isEmpty()) {
            NodeEdgeCostLen cur = queue.poll();
            if (proposals.isEmpty() && (cur.c>lastCost+epsilon || cur.l>lastLen+epsilon)) break;
            lastCost = cur.c;
            lastLen = cur.l;
            CostLenEdgeSet curAddDNS = added.get(cur.n);
            if (curAddDNS==null) {
                curAddDNS = new CostLenEdgeSet(cur.c, cur.l);
                added.put(cur.n, curAddDNS);
                for (Edge e: cur.n.getOutEdges()) {
                    double newC = cur.c+wg.getWeight(e);
                    double newL = cur.l+e.getLength();
                    CostLenEdgeSet newAddDNS = added.get(e.getStop());
                    CostLen newPropDNS = proposals.get(e.getStop());
                    if ((newAddDNS==null || (newC<newAddDNS.c+epsilon && newL<newAddDNS.l+epsilon))
                            && (newPropDNS==null || (newC<newPropDNS.c+epsilon && newL<newPropDNS.l+epsilon))) {
                        queue.add(new NodeEdgeCostLen(e.getStop(), e, newC, newL));
                        if (cur.l <= reach + epsilon) {
                            proposals.put(e.getStop(), new CostLen(newC, newL));
                        } else if (newPropDNS != null && (newC + epsilon < newPropDNS.c || newL + epsilon < newPropDNS.l)) {
                            proposals.remove(e.getStop());
                        }
                    }
                }
            }
            if (cur.c<curAddDNS.c+epsilon && cur.l<curAddDNS.l+epsilon) {
                if (cur.last!=null) curAddDNS.edges.add(cur.last);
                proposals.remove(cur.n);
                p.process(added, cur.n, cur.last, cur.l);
            }
        }
    }

    private class CostLenEdgeSet {
        private final double c;
        private final double l;
        private final Set<Edge> edges = new HashSet<>();
        private CostLenEdgeSet(double c, double l) {
            this.c = c;
            this.l = l;
        }
    }

    private class CostLen {
        private final double c;
        private final double l;
        private CostLen(double c, double l) {
            this.c = c;
            this.l = l;
        }
    }

    private class NodeEdgeCostLen {
        private Node n;
        private Edge last;
        private double c;
        private double l;
        private NodeEdgeCostLen(Node n, Edge last, double c, double l) {
            this.n = n;
            this.last = last;
            this.c = c;
            this.l = l;
        }
    }

    private class NodePairLen {
        private NodePair n;
        private double l;
        private NodePairLen(NodePair n, double l) {
            this.n = n;
            this.l = l;
        }
    }

    @Override
    public Graph getSubgraph(Node start, double len) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Find starting nodes
        HashSet<NodePair> startPairs = new HashSet<>();
        Queue<NodePairLen> q = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        contract(start, (added, lastNode, lastEdge, pLen) -> {
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(lastEdge);
                edgeList.add(lastEdge);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                Collections.reverse(edgeList);
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).l>reach+epsilon && pLen-added.get(e.getStop()).l<=reach+epsilon) {
                        NodePair np = null;
                        if (e.getStop()==lastNode && !bi) np = hyperNodes.getNodePair(e.getStart(), lastNode);
                        if (np==null) np = hyperNodes.getNodePair(e.getStop(), lastNode);
                        if (np==null) throw new IllegalArgumentException("Expected nodepairs are missing!");
                        startPairs.add(np);
                        q.add(new NodePairLen(np, pLen));
                    }
                }
            }
        });
        // Match those starting nodes to nodes from the actual graph
        HashMap<NodePair, Double> forwardLens = new HashMap<>();
        // Forward Dijkstra
        double curLen = -1;
        while (curLen<=len+epsilon && !q.isEmpty()) {
            NodePairLen curNl = q.poll();
            NodePair curNode = curNl.n;
            curLen = curNl.l;
            if (!forwardLens.containsKey(curNode)) {
                forwardLens.put(curNode, curLen);
                for (Edge e: curNode.getOutEdges()) {
                    double eTourLen = curLen+e.getLength() + dc.getDistance(e.getStop(), start);
                    if (!forwardLens.containsKey(e.getStop()) && eTourLen<=len+epsilon) {
                        q.add(new NodePairLen((NodePair) e.getStop(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Backward Dijkstra
        HashMap<NodePair, Double> backwardLens = new HashMap<>();
        q.clear();
        for (Node n: getNodes().values()) {
            NodePair np = (NodePair) n;
            if (np.e == start) {
                q.add(new NodePairLen(np, 0));
            }
        }
        curLen = -1;
        while (curLen<=len+epsilon && !q.isEmpty()) {
            NodePairLen curNl = q.poll();
            NodePair curNode = curNl.n;
            curLen = curNl.l;
            if (!backwardLens.containsKey(curNode)) {
                backwardLens.put(curNode, curLen);
                for (Edge e: curNode.getInEdges()) {
                    double eTourLen = curLen + e.getLength() + dc.getDistance(start, e.getStart());
                    if (!backwardLens.containsKey(e.getStart()) && eTourLen<=len+epsilon) {
                        q.add(new NodePairLen((NodePair) e.getStart(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Extract subgraph
        Graph out = new Graph();
        GraphNodePairSet nps = new GraphNodePairSet(out);
        for (Map.Entry<NodePair, Double> nd: forwardLens.entrySet()) {
            Double backwardLen = backwardLens.get(nd.getKey());
            if (backwardLen!=null && nd.getValue()+backwardLen<=len+epsilon) {
                nps.addNodePair(nd.getKey().s, nd.getKey().e);
            }
        }
        for (Node n: forwardLens.keySet()) {
            NodePair np0 = nps.getNodePair((NodePair) n);
            if (np0!=null) {
                for (Edge e: n.getOutEdges()) {
                    NodePair np1 = nps.getNodePair((NodePair) e.getStop());
                    if (np1!=null) {
                        new SimpleEdge(e, np0, np1);
                    }
                }
            }
        }
        // Add extra nodes to subgraph
        HashSet<NodePair> addedStartPairs = new HashSet<>();
        for (NodePair np: startPairs) {
            NodePair npAdded = nps.getNodePair(np.s, np.e);
            if (npAdded!=null) {
                addedStartPairs.add(np);
            }
        }
        contract(start, (added, lastNode, lastEdge, pLen) -> {
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(lastEdge);
                edgeList.add(lastEdge);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                boolean successors = false;
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).l>reach+epsilon && pLen-added.get(e.getStop()).l<=reach+epsilon) {
                        NodePair np1 = null;
                        if (e.getStop()==lastNode && !bi) np1 = nps.getNodePair(e.getStart(), lastNode);
                        if (np1==null) np1 = nps.getNodePair(e.getStop(), lastNode);
                        if (np1!=null) {
                            successors = true;
                            addedStartPairs.remove(np1);
                            NodePair np0 = nps.addNodePair(start, lastEdge.getStart());
                            boolean add = true;
                            for (Edge e0: np1.getInEdges()) {
                                if (e0.getStart()==np0 && e0.getId()==e.getId()) add = false;
                            }
                            if (add) {
                                new SimpleEdge(lastEdge, np0, np1);
                            }
                        }
                    }
                }
                if (successors) {
                    edgeList.remove(0);
                    Collections.reverse(edgeList);
                    for (Edge e: edgeList) {
                        NodePair np0 = nps.addNodePair(start, e.getStart());
                        NodePair np1 = nps.addNodePair(start, e.getStop());
                        boolean add = true;
                        for (Edge e0: np1.getInEdges()) {
                            if (e0.getStart()==np0 && e0.getId()==e.getId()) add = false;
                        }
                        if (add) {
                            new SimpleEdge(e, np0, np1);
                        }
                    }
                }
            }
        });
        if (!addedStartPairs.isEmpty()) throw new IllegalArgumentException("Not all startnodepairs were coupled to the graph!");
        /*HashSet<Integer> ids = new HashSet<>();
        for (Node n: out.getNodes().values()) for (Edge e: n.getOutEdges()) ids.add(e.id);
        JSONObject o = new JSONObject();
        JsonWriter w = new JsonWriter(o);
        Tree t = new Tree();
        for (Node n: g.getNodes().values()) {
            for (Edge e: n.getOutEdges()) if (ids.contains(e.id)) new Tree.TreeNode(t.getRoot(), null, e).connect();
        }
        t.writeLong(o);
        w.write("contract.json");*/
        return out;
    }

    public HashMap<Node, LinkedList<Edge>> getSubgraphFast(Node start) {
        HashMap<Node, LinkedList<Edge>> out = new HashMap<>();
        NodePairSet nps = new NodePairSet();
        // Forward Dijkstra
        contract(start, (added, lastNode, lastEdge, pLen) -> {
            if (pLen>reach+epsilon && pLen-lastEdge.getLength()<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(lastEdge);
                edgeList.add(lastEdge);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).l>reach+epsilon && pLen-added.get(e.getStop()).l<=reach+epsilon) {
                        NodePair np0 = hyperNodes.getNodePair(start, lastEdge.getStart());
                        if (np0==null) {
                            NodePair np1 = null;
                            if (e.getStop()==lastNode && !bi) np1 = hyperNodes.getNodePair(e.getStart(), lastNode);
                            if (np1==null) np1 = hyperNodes.getNodePair(e.getStop(), lastNode);
                            np0 = nps.addNodePair(start, lastEdge.getStart());
                            LinkedList<Edge> np0Out = out.get(np0);
                            if (np0Out==null) {
                                np0Out = new LinkedList<>();
                                out.put(np0, np0Out);
                            }
                            boolean add = true;
                            for (Edge e0 : np0Out) {
                                if (e0.getStop() == np1 && e0.getId() == e.getId()) add = false;
                            }
                            if (add) {
                                Edge e_new = SimpleEdge.getUncoupledEdge(lastEdge, np0, np1);
                                np0Out.add(e_new);
                            }
                        }
                    }
                }
                edgeList.remove(0);
                Collections.reverse(edgeList);
                for (Edge e: edgeList) {
                    NodePair np0 = hyperNodes.getNodePair(start, e.getStart());
                    if (np0==null) {
                        np0 = nps.addNodePair(start, e.getStart());
                        NodePair np1 = hyperNodes.getNodePair(start, e.getStop());
                        if (np1 == null) np1 = nps.addNodePair(start, e.getStop());
                        LinkedList<Edge> np0Out = out.get(np0);
                        if (np0Out==null) {
                            np0Out = new LinkedList<>();
                            out.put(np0, np0Out);
                        }
                        boolean add = true;
                        for (Edge e0 : np0Out) {
                            if (e0.getStop() == np1 && e0.getId() == e.getId()) add = false;
                        }
                        if (add) {
                            Edge e_new = SimpleEdge.getUncoupledEdge(e, np0, np1);
                            np0Out.add(e_new);
                        }
                    }
                }
            }
        });
        return out;
    }

    private class NodePairSet {
        private final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private NodePairSet() {}

        private NodePair addNodePair(Node s, Node e) {
            NodePair last = new NodePair(0, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }
    }
}
