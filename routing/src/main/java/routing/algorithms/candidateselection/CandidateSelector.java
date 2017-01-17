package routing.algorithms.candidateselection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.graph.Edge;
import routing.graph.Node;
import routing.graph.SPGraph;
import routing.graph.Tree;
import sun.awt.image.ImageWatched;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by piete on 18/04/2016.
 */
public abstract class CandidateSelector {
    protected Random r = new Random();
    protected ArrayList<Candidate> candidates;
    double[] probabilities;

    public CandidateSelector() {}

    public void initialize(Collection<Candidate> candidates) {
        this.candidates = new ArrayList<>(candidates);
        probabilities = new double[candidates.size()];
    }

    public abstract Candidate selectCandidate();

    public LinkedList<Candidate> selectCandidates(int nr) {
        LinkedList<Candidate> out = new LinkedList<Candidate>();
        for (int i=0; i<nr; i++) {
            //JSONObject jo = toJSON();
            //JsonWriter jw = new JsonWriter(jo);
            //jw.write("nodes_" + i + ".json");
            Candidate c = selectCandidate();
            if (c==null) break;
            else out.add(c);
        }
        return out;
    }

    protected Candidate selectCandidateUsingProbabilities() {
        double sum = 0;
        for (int i = 0; i<probabilities.length; i++) sum += probabilities[i];
        if (sum==0) return null;
        double chosen = r.nextDouble()*sum;
        sum = 0;
        for (int i = 0; i<probabilities.length; i++) {
            sum += probabilities[i];
            if (sum>chosen) return candidates.get(i);
        }
        throw new IllegalAccessError("Unable to choose a random number for these probabilities");
    }

    public JSONObject toJSON() {
        // Normalize probabilities
        double max = 0;
        for (double d : probabilities) if (d>max) max = d;
        // Collect probabilities per node
        HashMap<Node, ArrayList<Double>> probsPerNode = new HashMap<>();
        for (int i = 0; i<candidates.size(); i++) {
            Node n = ((SPGraph.NodePair) candidates.get(i).node.getNode()).e;
            ArrayList<Double> probs = probsPerNode.computeIfAbsent(n, k->new ArrayList<>());
            probs.add(probabilities[i]/max);
        }
        // Put everything in a JSONObject
        JSONObject out = new JSONObject();
        JSONArray nodes = new JSONArray();
        out.put("nodes", nodes);
        for (Map.Entry<Node, ArrayList<Double>> en: probsPerNode.entrySet()) {
            JSONObject node = en.getKey().toJSON();
            ArrayList<Double> probs = en.getValue();
            probs.sort(Comparator.naturalOrder());
            double max_prob = probs.get(probs.size()-1);
            if (max_prob>0) {
                node.put("max_prob", probs.get(probs.size()-1));
                node.put("min_prob", probs.get(0));
                node.put("probs", probs.toString());
                nodes.add(node);
            }
        }
        return out;
    }
}