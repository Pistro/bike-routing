package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import routing.IO.JsonWriter;
import routing.IO.NodeReader;
import routing.IO.NodeWriter;
import routing.algorithms.exact.*;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.Path;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLengthEBatch extends Command {
    private WeightBalancer wb;
    private double minLength;
    private double maxLength;
    private double lambda;
    private double s;
    private String batchIn;
    private String out;
    private double wbReach;
    private double reach;
    private long time;
    private int nrThreads;

    // Batch nodes
    private HashMap<Node, HashMap<String, Object>> nodeInfo;
    private Node[] nodes;
    private int nextNode = 0;

    // Graphs
    private Graph g;
    private SPGraph hyper;

    public FindLengthEBatch() {}

    public FindLengthEBatch(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "findLengthEBatch";
    }

    protected void initialize(ArgParser ap) {
        // Required arguments
        batchIn = ap.getString("batchIn");
        reach = ap.getLong("reach");
        minLength = ap.getDouble("minLength");
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        // Optionals
        wb = new WeightBalancer(ap.getDouble("wFast", 0), ap.getDouble("wAttr", 0.5), ap.getDouble("wSafe", 0.5));
        wbReach = ap.getDouble("wbReach", 0.5);
        s = ap.getDouble("s", 0.4);
        lambda = ap.getDouble("lambda", 0.01);
        time = ap.getLong("time", 60*60*1000);
        nrThreads = ap.getInt("threads", 16);
    }

    public synchronized Node getNode() {
        if (nextNode<nodes.length) {
            System.out.println("Processing " + nextNode + "/" + (nodes.length - 1) + " " + nodes[nextNode].getId());
            return nodes[nextNode++];
        } else return null;
    }

    public void execute(Graph g) {
        this.g = g;
        System.out.println("Reading & matching batch nodes...");
        try {
            long start = System.currentTimeMillis();
            JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader(batchIn));
            nodeInfo = new NodeReader(g, (JSONArray) obj.get("nodes")).nodes;
            nodes = new Node[nodeInfo.size()];
            int idx = 0;
            for (Node n: nodeInfo.keySet()) nodes[idx++] = n;
            long stop = System.currentTimeMillis();
            System.out.println("Nodes ready! Read & matching time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Creating hypergraph...");
            start = System.currentTimeMillis();
            hyper = new SPGraph(g, reach, false, wb, wbReach);
            stop = System.currentTimeMillis();
            System.out.println("Hypergraph created (" + hyper.getNodes().size() + " nodes)! Creation time: " + 1.0 * (stop - start) / 1000 + "s");
            System.out.println("Starting routing (length: " + minLength/1000 + "-" + maxLength/1000 + "km)...");
            Thread[] threads = new Thread[nrThreads];
            for(int i = 0; i < nrThreads; i++) {
                threads[i] = new Thread(new findLengthEThread());
                threads[i].start();
            }
            try {
                for (Thread thread : threads)
                    thread.join();
                System.out.println("Routes found!");
                System.out.println("Writing routes...");
                start = System.currentTimeMillis();
                NodeWriter nw  = new NodeWriter(nodeInfo);
                JSONObject nodes = new JSONObject();
                nodes.put("nodes", nw.toJSON());
                JsonWriter jw = new JsonWriter(nodes);
                jw.write(out);
                stop = System.currentTimeMillis();
                System.out.println("Routes written! Writing time: " + 1.0 * (stop - start) / 1000 + "s");
            } catch (InterruptedException e) {
                System.out.println("Execution interrupted");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class findLengthEThread implements Runnable {
        public findLengthEThread() {
        }

        public void run() {
            Node n = getNode();
            while (n != null) {
                processNode(n);
                n = getNode();
            }
        }

        public void processNode(Node n) {
            ExhaustiveRouteLengthFinder rlf = new ExhaustiveRouteLengthFinder(n, wb, lambda, s, minLength, maxLength, hyper);
            rlf.verbose = false;
            rlf.maxSearchTimeMs = time;
            long start = System.currentTimeMillis();
            Path p = rlf.findRoute();
            long stop = System.currentTimeMillis();
            HashMap<String, Object> map = nodeInfo.get(n);
            if (!map.containsKey("tag_results")) map.put("tag_results", new JSONArray());
            JSONArray results = (JSONArray) map.get("tag_results");
            JSONObject result = new JSONObject();
            JSONObject configuration = new JSONObject();
            configuration.put("algorithm", "exact");
            configuration.put("reach", reach);
            configuration.put("minLength", minLength);
            configuration.put("maxLength", maxLength);
            configuration.put("strictness", s);
            configuration.put("lambda", lambda);
            JSONObject output = new JSONObject();
            output.put("time", 1.0*(stop-start)/1000);
            output.put("scoreBound", rlf.getScore());
            output.put("nrIter", rlf.getNrIterations());
            if (p!=null) {
                double weight = p.getWeight(wb) / p.getLength();
                double interference = lambda * p.getInterference(s) / p.getLength();
                double score = weight + interference;
                p.addTag("length", p.getLength());
                p.addTag("weight", weight);
                p.addTag("interf", interference);
                p.addTag("score", score);
                output.put("route", p.toJSON());
            }
            result.put("configuration", configuration);
            result.put("output", output);
            results.add(result);
        }
    }
}
