package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.IO.JsonWriter;
import routing.algorithms.exact.*;
import routing.graph.Graph;
import routing.graph.Path;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLengthE extends Command {
    WeightBalancer wb;
    long startId;
    double minLength;
    double maxLength;
    String out;
    private double lambda;
    private double s;
    long time;
    double reach;
    double wbReach;

    public FindLengthE() {}

    public FindLengthE(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "findLengthE";
    }

    protected void initialize(ArgParser ap) {
        // Required arguments
        startId = ap.getLong("startId");
        reach = ap.getLong("reach");
        minLength = ap.getDouble("minLength");
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        // Optionals
        wb = new WeightBalancer(ap.getDouble("wFast", 0), ap.getDouble("wAttr", 0.5), ap.getDouble("wSafe", 0.5));
        s = ap.getDouble("s", 0.4);
        lambda = ap.getDouble("lambda", 0.01);
        time = ap.getLong("time", -1);
        wbReach = ap.getDouble("wbReach", 0.5);
    }

    public void execute(Graph g) {
        System.out.println("Extracting subgraph...");
        long start = System.currentTimeMillis();
        Graph g2 = g.getSubgraph(g.getNode(startId), maxLength);
        long stop = System.currentTimeMillis();
        System.out.println("Extraction finished! Extraction time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Creating hypergraph...");
        start = System.currentTimeMillis();
        SPGraph g3 = new SPGraph(g2, reach, false, wb, wbReach);
        stop = System.currentTimeMillis();
        System.out.println("Hypergraph created! Creation time: " + 1.0*(stop-start)/1000 + "s");
        System.out.println("Starting routing (length: " + minLength/1000 + "-" + maxLength/1000 + "km)...");
        ExhaustiveRouteLengthFinder rlf = new ExhaustiveRouteLengthFinder(g2.getNode(startId), wb, lambda, s, minLength, maxLength, g3);
        rlf.maxSearchTimeMs = time;
        start = System.currentTimeMillis();
        Path p = rlf.findRoute();
        stop = System.currentTimeMillis();
        System.out.println("Routing finished! Routing time: " + 1.0*(stop-start)/1000 + "s");
        if (p!=null) {
            System.out.println("Writing routes to '" + out + "'...");
            JSONObject j = new JSONObject();
            double weight = p.getWeight(wb) / p.getLength();
            double interference = lambda * p.getInterference(s) / p.getLength();
            p.addTag("weightE", Double.toString(weight));
            p.addTag("interfE", Double.toString(interference));
            p.addTag("scoreE", Double.toString(weight + interference));
            JSONArray routes = new JSONArray();
            routes.add(p.toJSON());
            j.put("routes", routes);
            new JsonWriter(j).write(out);
            System.out.println("Routes written!");
        } else {
            if (rlf.getScore()==Double.MAX_VALUE) System.out.println("No route found!");
            else System.out.println("Score under bound: " + rlf.getScore());
        }
    }
}
