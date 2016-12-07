package routing.algorithms.candidateselection;

import routing.graph.Tree;

/**
 * Created by piete on 18/04/2016.
 */
public class Candidate {
    public Tree.TreeNode node;
    public double weight;
    public double length;
    public Candidate(Tree.TreeNode node, double weight, double length) {
        this.node = node;
        this.weight = weight;
        this.length = length;
    }
}
