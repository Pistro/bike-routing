package routing.algorithms.exact;

import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
 * Simple shortest path tree based method to determine reaches up to a maximal length dMax.
 * Created by Pieter on 1/03/2016.
 */
public class LengthReachFinder {
    private int nextNode = 0;
    private Node[] nodesToProcess;
    private double dMax;
    private WeightGetter wg;
    private SPGraph hyper;

    public LengthReachFinder(Graph g, SPGraph hyper, WeightGetter wg, double dMax, int nrThreads) {
        this.hyper = hyper;
        this.dMax = dMax;
        this.wg = wg;
        nodesToProcess = new Node[g.getOrder()];
        int pos = 0;
        for (Node n: g.getNodes().values()) {
            nodesToProcess[pos] = n;
            pos++;
        }

        Thread[] threads = new Thread[nrThreads];
        for(int i = 0; i < nrThreads; i++) {
            threads[i] = new Thread(new reachThread());
            threads[i].start();
        }
        try {
            for (Thread thread : threads)
                thread.join();
            nodesToProcess = null;
        } catch (InterruptedException e) {
            System.out.println("Execution interrupted");
        }
        for (Node n: hyper.getNodes().values()) {
            if (n.getReach()>dMax) n.clearReach();
        }
    }

    public synchronized Node getNode() {
        if (nextNode%1000==0) System.out.println(nextNode + "/" + nodesToProcess.length);
        if (nextNode<nodesToProcess.length) return nodesToProcess[nextNode++];
        else return null;
    }

    private class reachThread implements Runnable {
        private reachThread() {
        }

        public void run() {
            Node n = getNode();
            while (n !=null) {
                processNode(n);
                n = getNode();
            }
        }

        // Calculate distances to all nodes further than lowerbound and closer than upperbound from the neighbour of each node
        private void processNode(Node n) {
            // Find the neighbour that is the most far from n
            double maxLength = 2*dMax + getFurthestNeighbour(n);
            // Now execute modified Dijkstra
            Tree t = getNearbyNodes(n, maxLength);
            // Use the resulting shortest path tree to update...
            Tree.TreeNode r = t.getRoot().getChildren().get(0);
            updateTreeNode(r, 0);
        }

        private double getFurthestNeighbour(Node n) {
            double maxLength = 0;
            for (Edge e : n.getOutEdges())
                if (e.getLength() > maxLength) maxLength = e.getLength();
            return maxLength;
        }

        private class TreeNodeWeightLength {
            private Tree.TreeNode tn;
            private double length;
            private double weight;
            private TreeNodeWeightLength(Tree.TreeNode tn, double length, double weight) {
                this.tn = tn;
                this.length = length;
                this.weight = weight;
            }
            private double getWeight() { return weight; }
        }

        Tree getNearbyNodes(Node nOrg, double maxLength) {
            HashMap<Node, LinkedList<Edge>> nearbyNodes = hyper.getSubgraphFast(nOrg);
            Node start = hyper.getNodePair(nOrg, nOrg);
            if (start==null) start = new SPGraph.NodePair(0, nOrg, nOrg);
            Tree tree = new Tree();
            PriorityQueue<TreeNodeWeightLength> candidates = new PriorityQueue<>(Comparator.comparing(TreeNodeWeightLength::getWeight));
            HashSet<Node> toReach = new HashSet<>();
            HashSet<Node> addedNodes = new HashSet<>();
            HashMap<Node, Double> nodeToWeight = new HashMap<>();
            candidates.add(new TreeNodeWeightLength(new Tree.TreeNode(tree.getRoot(), start, null), 0., 0.));
            toReach.add(start);
            nodeToWeight.put(start, 0.);
            while(!toReach.isEmpty()) {
                TreeNodeWeightLength cur = candidates.poll();
                Tree.TreeNode curTreeNode = cur.tn;
                Node curNode = curTreeNode.getNode();
                if (addedNodes.add(curNode)) {
                    curTreeNode.connect();
                    toReach.remove(curNode);
                    LinkedList<Edge> neighbours = nearbyNodes.get(curNode);
                    if (neighbours == null) neighbours = curNode.getOutEdges();
                    for (Edge e : neighbours) {
                        if (!addedNodes.contains(e.getStop())) {
                            double newWeight = cur.weight + wg.getWeight(e);
                            Double oldWeight = nodeToWeight.get(e.getStop());
                            if (oldWeight == null || oldWeight>newWeight) {
                                double newLength = cur.length + e.getLength();
                                nodeToWeight.put(e.getStop(), newWeight);
                                candidates.add(new TreeNodeWeightLength(new Tree.TreeNode(curTreeNode, e.getStop(), e), newLength, newWeight));
                                if (newLength > maxLength) toReach.remove(e.getStop());
                                else toReach.add(e.getStop());
                            }
                        }
                    }
                }
            }
            return tree;
        }

        private double updateTreeNode(Tree.TreeNode n, double distanceFromRoot) {
            double maxDistFromLeaf = -1;
            for (Tree.TreeNode child: n.getChildren()) {
                double edgeLength = child.getEdgeFromParent().getLength();
                double distFromLeaf = updateTreeNode(child, distanceFromRoot+edgeLength)+edgeLength;
                if (distFromLeaf>maxDistFromLeaf) maxDistFromLeaf = distFromLeaf;
            }
            n.getNode().setReach(Math.min(maxDistFromLeaf, distanceFromRoot));
            return maxDistFromLeaf;
        }

    }
}
