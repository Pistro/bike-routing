package routing.graph;

import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.heuristics.DistanceCalculator;
import routing.graph.weights.WeightBalancer;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 * Created by pieter on 24/10/2016.
 */
public class SPGraph extends Graph {
    private final double reach;
    private static final double epsilon = 0.00001;
    private final WeightGetter wg;
    private final NodePairSet hyperNodes;

    public NodePair getNodePair(Node start, Node stop) {
        return hyperNodes.getNodePair(start, stop);
    }

    private class NodePairSet {
        private long idCnt = 0;
        private final Graph gr;
        private final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private NodePairSet(Graph gr) {
            this.gr = gr;
        }

        private NodePair addNodePair(Node s, Node e) {
            NodePair last = new NodePair(idCnt++, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                gr.addNode(last);
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }

        private NodePair getNodePair(Node s, Node e) {
            return pairs.get(new NodePair(idCnt++, s, e));
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
            int hash = 1;
            hash = hash * 17 + s.hashCode();
            hash = hash * 31 + e.hashCode();
            return hash;
        }
        @Override
        public boolean equals(Object aThat) {
            if ( this == aThat ) return true;
            if ( !(aThat instanceof NodePair) ) return false;
            NodePair that = (NodePair)aThat;
            return s == that.s && e == that.e;
        }
    }

    public SPGraph(Graph g, double reach, boolean bidirectional, WeightBalancer wb, double wbInfluence) {
        this.reach = reach;
        double sum = wb.getWAttr()+wb.getWFast()+wb.getWSafe();
        this.wg = new WeightBalancer((1-wbInfluence)+wbInfluence*wb.getWFast()/sum, wbInfluence*wb.getWAttr()/sum, wbInfluence*wb.getWSafe()/sum);

        // Construct hypergraph using every possible starting node
        hyperNodes = new NodePairSet(this);
        int i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            HashMap<Node, Double> added = new HashMap<>();
            HashMap<Node, DoubleEdgeSet> proposals = new HashMap<>();
            Queue<NodeEdgeLen> queue = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
            queue.add(new NodeEdgeLen(n, null, 0));
            while (!proposals.isEmpty() || added.isEmpty()) {
                NodeEdgeLen cur = queue.poll();
                Double curAdd = added.get(cur.n);
                if (curAdd==null) {
                    added.put(cur.n, cur.l);
                    curAdd = cur.l;
                    for (Edge e: cur.n.getInEdges()) {
                        double newL = cur.l+wg.getWeight(e);
                        Double addedLength = added.get(e.getStart());
                        if (addedLength==null || newL<addedLength+epsilon) {
                            queue.add(new NodeEdgeLen(e.getStart(), e, newL));
                            DoubleEdgeSet newDNS = proposals.get(e.getStart());
                            if (newDNS != null && newL + epsilon < newDNS.d) {
                                proposals.remove(e.getStart());
                                newDNS = null;
                            }
                            if (cur.l <= reach + epsilon) {
                                if (newDNS == null) {
                                    newDNS = new DoubleEdgeSet(newL);
                                    proposals.put(e.getStart(), newDNS);
                                }
                                if (newL < newDNS.d + epsilon) newDNS.edges.add(e);
                            }
                        }
                    }
                }
                if (cur.l<curAdd+epsilon && cur.last!=null) {
                    if (cur.l-wg.getWeight(cur.last) <= reach + epsilon) {
                        DoubleEdgeSet curDNS = proposals.get(cur.n);
                        curDNS.edges.remove(cur.last);
                        if (curDNS.edges.isEmpty()) proposals.remove(cur.n);
                    }
                    if (cur.l>reach+epsilon && cur.l-wg.getWeight(cur.last)<=reach+epsilon) {
                        hyperNodes.addNodePair(cur.last.getStop(), n);
                    }
                }
            }
        }
        i = 0;
        for (Node n: g.getNodes().values()) {
            if (++i%1000==0) System.out.println(i + "/" + g.getNodes().size());
            contract(n, (added, last, pLen) -> {
                if (pLen>reach+epsilon && pLen-wg.getWeight(last)<=reach+epsilon) {
                    Set<Edge> edgeSet = new HashSet<>();
                    ArrayList<Edge> edgeList = new ArrayList<>();
                    edgeSet.add(last);
                    edgeList.add(last);
                    for (int j = 0; j<edgeList.size(); j++) {
                        for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                            if (edgeSet.add(e)) edgeList.add(e);
                        }
                    }
                    edgeList.remove(0);
                    Collections.reverse(edgeList);
                    for (Edge e: edgeList) {
                        NodePair np0 = hyperNodes.getNodePair(n, e.getStart());
                        if (np0!=null) {
                            NodePair np1 = hyperNodes.addNodePair(n, e.getStop());
                            boolean add = true;
                            for (Edge e0: np1.getInEdges()) {
                                if (e0.getStart()==np0 && e0.id==e.id) add = false;
                            }
                            if (add) {
                                Edge e_new = new Edge(e, np0, np1);
                                e_new.id = e.id;
                            }
                        }
                    }
                    edgeList.add(last);
                    for (Edge e: edgeList) {
                        if (pLen-added.get(e.getStart()).d>reach+epsilon && pLen-added.get(e.getStop()).d<=reach+epsilon) {
                            NodePair np0 = hyperNodes.getNodePair(n, last.getStart());
                            if (np0!=null) {
                                NodePair np1 = hyperNodes.getNodePair(e.getStop(), last.getStop());
                                if (np1==null) {
                                    throw new IllegalArgumentException("Maximal history nodepair missing!");
                                }
                                Edge e_new = new Edge(last, np0, np1);
                                e_new.id = last.id;
                            }
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
                for (Edge e_in : new LinkedList<>(cur.getInEdges())) {
                    NodePair n_in = (NodePair) e_in.getStart();
                    Edge matching_out = null;
                    for (Edge e_out : cur.getOutEdges()) {
                        NodePair n_out = (NodePair) e_out.getStop();
                        if (n_in.e == n_out.e) {
                            matching_out = e_out;
                            break;
                        }
                    }
                    if (matching_out != null) {
                        Node n_new = hyperNodes.addNodePair(new SimpleNode(), curNp.e);
                        Edge e_in_new = new Edge(e_in, e_in.getStart(), n_new);
                        e_in_new.id = e_in.id;
                        for (Edge e_out : cur.getOutEdges()) {
                            if (e_out != matching_out) {
                                Edge e_new = new Edge(e_out, n_new, e_out.getStop());
                                e_new.id = e_out.id;
                            }
                        }
                        e_in.decouple();
                    }
                }
            }
        }
    }

    private interface TreeNodeProcessor {
        void process(HashMap<Node, DoubleEdgeSet> added, Edge last, double pLen);
    }

    private void contract(Node n, TreeNodeProcessor p) {
        HashMap<Node, DoubleEdgeSet> added = new HashMap<>();
        HashMap<Node, DoubleEdgeSet> proposals = new HashMap<>();
        Queue<NodeEdgeLen> queue = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        queue.add(new NodeEdgeLen(n, null, 0));
        while (!proposals.isEmpty() || added.isEmpty()) {
            NodeEdgeLen cur = queue.poll();
            DoubleEdgeSet curAddDNS = added.get(cur.n);
            if (curAddDNS==null) {
                curAddDNS = new DoubleEdgeSet(cur.l);
                added.put(cur.n, curAddDNS);
                for (Edge e: cur.n.getOutEdges()) {
                    double newL = cur.l+wg.getWeight(e);
                    DoubleEdgeSet newAddDNS = added.get(e.getStop());
                    if (newAddDNS==null || newL<newAddDNS.d+epsilon) {
                        queue.add(new NodeEdgeLen(e.getStop(), e, newL));
                        DoubleEdgeSet newDNS = proposals.get(e.getStop());
                        if (newDNS != null && newL + epsilon < newDNS.d) {
                            proposals.remove(e.getStop());
                            newDNS = null;
                        }
                        if (cur.l <= reach + epsilon) {
                            if (newDNS == null) {
                                newDNS = new DoubleEdgeSet(newL);
                                proposals.put(e.getStop(), newDNS);
                            }
                            if (newL < newDNS.d + epsilon) newDNS.edges.add(e);
                        }
                    }
                }
            }
            if (cur.l<curAddDNS.d+epsilon) {
                if (cur.last!=null) curAddDNS.edges.add(cur.last);
                if (cur.last!=null && cur.l-wg.getWeight(cur.last) <= reach + epsilon) {
                    DoubleEdgeSet curDNS = proposals.get(cur.n);
                    curDNS.edges.remove(cur.last);
                    if (curDNS.edges.isEmpty()) proposals.remove(cur.n);
                }
                p.process(added, cur.last, cur.l);
            }
        }
    }

    private class DoubleEdgeSet {
        private final double d;
        private final Set<Edge> edges = new HashSet<>();
        private DoubleEdgeSet(double d) {
            this.d = d;
        }
    }

    private class NodeEdgeLen {
        private Node n;
        private Edge last;
        private double l;
        private NodeEdgeLen(Node n, Edge last, double l) {
            this.n = n;
            this.last = last;
            this.l = l;
        }
    }

    private class NodeLen {
        private Node n;
        private double l;
        private NodeLen(Node n, double l) {
            this.n = n;
            this.l = l;
        }
    }

    @Override
    public Graph getSubgraph(Node start, double len) {
        DistanceCalculator dc = new DistanceCalculator(start);
        // Forward Dijkstra
        HashSet<NodePair> startPairs = new HashSet<>();
        HashMap<Node, Double> forwardLens = new HashMap<>();
        contract(start, (added, last, pLen) -> {
            if (pLen>reach+epsilon && pLen-wg.getWeight(last)<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(last);
                edgeList.add(last);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                Collections.reverse(edgeList);
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).d>reach+epsilon && pLen-added.get(e.getStop()).d<=reach+epsilon) {
                        forwardLens.put(new NodePair(0, e.getStop(), last.getStop()), pLen);
                    }
                }
            }
        });
        Queue<NodeLen> q = new PriorityQueue<>((o1, o2) -> o1.l<o2.l? -1 : (o1.l>o2.l? 1 : 0));
        for (Node n: getNodes().values()) {
            NodePair np = (NodePair) n;
            Double l = forwardLens.get(np);
            if (l!=null) {
                forwardLens.remove(n);
                startPairs.add((NodePair) n);
                q.add(new NodeLen(n, l));
            }
        }
        if (!forwardLens.isEmpty()) throw new IllegalArgumentException("Expected nodepairs are missing!");
        double curLen = -1;
        while (curLen<len && !q.isEmpty()) {
            NodeLen curNl = q.poll();
            Node curNode = curNl.n;
            curLen = curNl.l;
            if (!forwardLens.containsKey(curNode)) {
                forwardLens.put(curNode, curLen);
                for (Edge e: curNode.getOutEdges()) {
                    double eTourLen = curLen+e.getLength() + dc.getDistance(e.getStop(), start);
                    if (!forwardLens.containsKey(e.getStop()) && eTourLen<len) {
                        q.add(new NodeLen(e.getStop(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Backward Dijkstra
        HashMap<Node, Double> backwardLens = new HashMap<>();
        q.clear();
        for (Node n: getNodes().values()) {
            NodePair np = (NodePair) n;
            if (np.e == start) {
                q.add(new NodeLen(n, 0));
            }
        }
        curLen = -1;
        while (curLen<len && !q.isEmpty()) {
            NodeLen curNl = q.poll();
            Node curNode = curNl.n;
            curLen = curNl.l;
            if (!backwardLens.containsKey(curNode)) {
                backwardLens.put(curNode, curLen);
                for (Edge e: curNode.getInEdges()) {
                    double eTourLen = curLen + e.getLength() + dc.getDistance(start, e.getStart());
                    if (!backwardLens.containsKey(e.getStart()) && eTourLen<len) {
                        q.add(new NodeLen(e.getStart(), curLen+e.getLength()));
                    }
                }
            }
        }
        // Extract subgraph
        Graph out = new Graph();
        NodePairSet nps = new NodePairSet(out);
        for (Map.Entry<Node, Double> nd: forwardLens.entrySet()) {
            Double backwardLen = backwardLens.get(nd.getKey());
            if (backwardLen!=null && nd.getValue()+backwardLen<=len) {
                nps.addNodePair(((NodePair) nd.getKey()).s, ((NodePair) nd.getKey()).e);
            }
        }
        for (Node n: forwardLens.keySet()) {
            NodePair np0 = nps.getNodePair((NodePair) n);
            if (np0!=null) {
                for (Edge e: n.getOutEdges()) {
                    NodePair np1 = nps.getNodePair((NodePair) e.getStop());
                    if (np1!=null) {
                        Edge e_new = new Edge(e, np0, np1);
                        e_new.id = e.id;
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
        contract(start, (added, last, pLen) -> {
            if (pLen>reach+epsilon && pLen-wg.getWeight(last)<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(last);
                edgeList.add(last);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                boolean successors = false;
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).d>reach+epsilon && pLen-added.get(e.getStop()).d<=reach+epsilon) {
                        NodePair np1 = nps.getNodePair(e.getStop(), last.getStop());
                        if (np1!=null) {
                            successors = true;
                            addedStartPairs.remove(np1);
                            NodePair np0 = nps.addNodePair(start, last.getStart());
                            boolean add = true;
                            for (Edge e0: np1.getInEdges()) {
                                if (e0.getStart()==np0 && e0.id==e.id) add = false;
                            }
                            if (add) {
                                Edge e_new = new Edge(last, np0, np1);
                                e_new.id = last.id;
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
                            if (e0.getStart()==np0 && e0.id==e.id) add = false;
                        }
                        if (add) {
                            Edge e_new = new Edge(e, np0, np1);
                            e_new.id = e.id;
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
        MapNodePairSet nps = new MapNodePairSet(out);
        // Forward Dijkstra
        contract(start, (added, last, pLen) -> {
            if (pLen>reach+epsilon && pLen-wg.getWeight(last)<=reach+epsilon) {
                Set<Edge> edgeSet = new HashSet<>();
                ArrayList<Edge> edgeList = new ArrayList<>();
                edgeSet.add(last);
                edgeList.add(last);
                for (int j = 0; j<edgeList.size(); j++) {
                    for (Edge e: added.get(edgeList.get(j).getStart()).edges) {
                        if (edgeSet.add(e)) edgeList.add(e);
                    }
                }
                for (Edge e: edgeList) {
                    if (pLen-added.get(e.getStart()).d>reach+epsilon && pLen-added.get(e.getStop()).d<=reach+epsilon) {
                        NodePair np0 = hyperNodes.getNodePair(start, last.getStart());
                        NodePair np1 = hyperNodes.getNodePair(e.getStop(), last.getStop());
                        if (np0==null) {
                            np0 = nps.addNodePair(start, last.getStart());
                            LinkedList<Edge> np0Out = out.get(np0);
                            boolean add = true;
                            for (Edge e0 : np0Out) {
                                if (e0.getStop() == np1 && e0.id == e.id) add = false;
                            }
                            if (add) {
                                Edge e_new = Edge.getUncoupledEdge(last, np0, np1);
                                e_new.id = last.id;
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
                        boolean add = true;
                        for (Edge e0 : np0Out) {
                            if (e0.getStop() == np1 && e0.id == e.id) add = false;
                        }
                        if (add) {
                            Edge e_new = Edge.getUncoupledEdge(e, np0, np1);
                            e_new.id = e.id;
                            np0Out.add(e_new);
                        }
                    }
                }
            }
        });
        return out;
    }

    private class MapNodePairSet {
        private long idCnt = -1;
        private final HashMap<Node, LinkedList<Edge>> map;
        private final HashMap<NodePair, NodePair> pairs = new HashMap<>();

        private MapNodePairSet(HashMap<Node, LinkedList<Edge>> map) {
            this.map = map;
        }

        private NodePair addNodePair(Node s, Node e) {
            NodePair last = new NodePair(idCnt--, s, e);
            NodePair lastHyper = pairs.get(last);
            if (lastHyper==null) {
                map.put(last, new LinkedList<>());
                pairs.put(last, last);
                lastHyper = last;
            }
            return lastHyper;
        }
    }
}
