package routing.algorithms.exact;

import datastructure.TreeHeap;
import routing.graph.*;
import routing.graph.weights.WeightGetter;

import java.util.*;

/**
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
        if (nextNode<nodesToProcess.length) return nodesToProcess[nextNode++];
        else return null;
    }

    private class reachThread implements Runnable {
        public reachThread() {
        }

        public void run() {
            Node n = getNode();
            while (n !=null) {
                processNode(n);
                n = getNode();
            }
        }

        // Calculate distances to all nodes further than lowerbound and closer upperbound from the neighbour of each node
        public void processNode(Node n) {
            // Find the neighbour that is the most far from n
            double maxLength = 2*dMax + getFurthestNeighbour(n);
            // Now execute modified Dijkstra
            Tree t = getNearbyNodes(n, maxLength);
            // Use the resulting shortest path tree to update...
            Tree.TreeNode r = t.getRoot().getChildren().get(0);
            updateTreeNode(r, 0);
        }

        public double getFurthestNeighbour(Node n) {
            double maxLength = 0;
            for (Edge e : n.getOutEdges())
                if (e.getLength() > maxLength) maxLength = e.getLength();
            return maxLength;
        }

        private class TreeNodeLength {
            Tree.TreeNode tn;
            double length;
            public TreeNodeLength(Tree.TreeNode tn, double length) {
                this.tn = tn;
                this.length = length;
            }
        }

        Tree getNearbyNodes(Node n, double maxLength) {
            HashMap<Node, LinkedList<Edge>> nearbyNodes = hyper.getSubgraphFast(n);
            n = hyper.getNodePair(n, n);
            Tree tree = new Tree();
            TreeHeap<TreeNodeLength> candidates = new TreeHeap<>();
            HashSet<Node> toReach = new HashSet<Node>();
            HashSet<Node> addedNodes = new HashSet<Node>();
            candidates.add(n.getId(), 0.0, new TreeNodeLength(new Tree.TreeNode(tree.getRoot(), n, null), 0.));
            toReach.add(n);
            while(!toReach.isEmpty()) {
                TreeHeap.TreeHeapElement<TreeNodeLength> cur = candidates.extractMin();
                Tree.TreeNode curTreeNode = cur.o.tn;
                curTreeNode.connect();
                Node curNode = curTreeNode.getNode();
                toReach.remove(curNode);
                addedNodes.add(curNode);
                LinkedList<Edge> neighbours = nearbyNodes.get(curNode);
                if (neighbours == null) neighbours = curNode.getOutEdges();
                for (Edge e: neighbours) {
                    if (!addedNodes.contains(e.getStop())) {
                        double newLength = cur.o.length+e.getLength();
                        TreeNodeLength tentativeNode = new TreeNodeLength(new Tree.TreeNode(curTreeNode, e.getStop(), e), newLength);
                        double newWeight = cur.weight+wg.getWeight(e);
                        TreeHeap.TreeHeapElement<TreeNodeLength> replaced = candidates.add(e.getStop().getId(), newWeight, tentativeNode);
                        if (replaced==null || replaced.o!=tentativeNode) {
                            if (newLength>maxLength) toReach.remove(e.getStop());
                            else toReach.add(e.getStop());
                        }
                    }
                }
            }
            return tree;
        }

        public double updateTreeNode(Tree.TreeNode n, double distanceFromRoot) {
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
