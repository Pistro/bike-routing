package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.candidateselection.CandidateSelector;
import routing.algorithms.candidateselection.DistPlSelector;
import routing.algorithms.heuristics.RouteLengthFinder;
import routing.graph.*;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;

import java.util.LinkedList;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLength extends Command {
    private WeightBalancer wb;
    private long startId;
    private double minLength;
    private double maxLength;
    private int alternatives;
    private String out;
    private double lambda;
    private double strictness;
    private double reach;
    private double wbReach;

    public FindLength() {}

    public FindLength(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "findLength";
    }

    protected void initialize(ArgParser ap) {
        // Required arguments
        startId = ap.getLong("startId");
        minLength = ap.getDouble("minLength");
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        reach = ap.getLong("reach");
        // Optionals
        alternatives = (int) ap.getLong("alt", 8);
        wb = new WeightBalancer(ap.getDouble("wFast", 0.33), ap.getDouble("wAttr", 0.33), ap.getDouble("wSafe", 0.33));
        wbReach = ap.getDouble("wbReach", 0.5);
        lambda = ap.getDouble("lambda", 0.01);
        strictness = ap.getDouble("strictness", 0.4);
    }

    public void execute(Graph g) {
        System.out.println("Extracting subgraph...");
        long start = System.currentTimeMillis();
        Graph g2 = g.getSubgraph(g.getNode(startId), maxLength);
        long stop = System.currentTimeMillis();
        System.out.println("Extraction finished! Extraction time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Creating hypergraph...");
        start = System.currentTimeMillis();
        SPGraph g3 = new SPGraph(g2, reach, true, wb, wbReach);
        stop = System.currentTimeMillis();
        System.out.println("Hypergraph created! Creation time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Starting routing (length: " + minLength/1000 + "-" + maxLength/1000 + "km, " + alternatives + " attempts)...");
        CandidateSelector cs = new DistPlSelector(g2.getNode(startId));
        RouteLengthFinder rlf = new RouteLengthFinder(wb, g2.getNode(startId), cs, minLength, maxLength, lambda, strictness, alternatives, g3);
        start = System.currentTimeMillis();
        LinkedList<Path> paths = rlf.findRoutes();
        stop = System.currentTimeMillis();
        System.out.println("Starting routing (length: " + minLength/1000 + "-" + maxLength/1000 + "km, " + alternatives + " attempts)! Routing time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Extraction time: " + rlf.extractionTime/1000. + "s");
        System.out.println("Forward time: " + rlf.forwardTime/1000. + "s");
        System.out.println("Backward time (avg): " + rlf.backwardTime/(1000.*alternatives) + "s");
        System.out.println("Writing routes to '" + out + "'...");
        JSONArray routes = new JSONArray();
        for (Path p : paths) {
            double weight = p.getWeight(wb)/p.getLength();
            double interference = lambda*p.getInterference(strictness)/p.getLength();
            p.addTag("weight", weight);
            p.addTag("interf", interference);
            p.addTag("score", weight+interference);
            routes.add(p.toJSON());
        }
        JSONObject j = new JSONObject();
        j.put("routes", routes);
        new JsonWriter(j).write(out);
        System.out.println("Routes written!");
    }
}
