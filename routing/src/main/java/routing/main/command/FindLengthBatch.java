package routing.main.command;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.*;
import routing.algorithms.candidateselection.CandidateSelector;
import routing.algorithms.candidateselection.DistPlSelector;
import routing.algorithms.heuristics.RouteLengthFinder;
import routing.graph.*;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by pieter on 5/06/2016.
 */
public class FindLengthBatch extends Command {
    private WeightBalancer wb;
    private WeightBalancer wbReach;
    private double minLength;
    private double maxLength;
    private double lambda;
    private double strictness;
    private String batchIn;
    private String out;
    private int alternatives;
    private String hyperIn;
    private double reach;

    public FindLengthBatch() {}

    public FindLengthBatch(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "findLengthBatch";
    }

    protected void initialize(ArgParser ap) {
        // Required arguments
        batchIn = ap.getString("batchIn", null);
        minLength = ap.getDouble("minLength");
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        // Choice
        hyperIn = ap.getString("hyperIn", null);
        reach = ap.getDouble("reach", -1);
        if (hyperIn==null && reach==-1) throw new IllegalArgumentException("Either reach or hyperIn should be specified!");
        // Optionals
        alternatives = (int) ap.getLong("alt", 8);
        wb = new WeightBalancer(ap.getDouble("wFast", 0.33), ap.getDouble("wAttr", 0.33), ap.getDouble("wSafe", 0.33));
        wbReach = new WeightBalancer(ap.getDouble("wbFast", 0.5), ap.getDouble("wbAttr", 0.25), ap.getDouble("wbSafe", 0.25));
        lambda = ap.getDouble("lambda", 0.01);
        strictness = ap.getDouble("strictness", 0.4);
    }

    public void execute(Graph g) {
        System.out.println("Reading & matching batch nodes...");
        try {
            long start = System.currentTimeMillis();
            JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader(batchIn));
            HashMap<Node, HashMap<String, Object>> nodeInfo = new NodeReader(g, (JSONArray) obj.get("nodes")).nodes;
            long stop = System.currentTimeMillis();
            System.out.println("Nodes ready! Read & matching time: " + 1.0 * (stop - start) / 1000 + "s");
            SPGraph g2;
            if (hyperIn!=null) {
                start = System.currentTimeMillis();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                XMLSPGraphReader gr = new XMLSPGraphReader(g);
                xmlReader.setContentHandler(gr);
                xmlReader.parse(Main.convertToFileURL(hyperIn));
                g2 = gr.getSpGraph();
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph Read! Reading time: " + 1.0 * (stop - start) / 1000 + "s");
            } else {
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g2 = new SPGraph(g, reach, false, wbReach);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + 1.0 * (stop - start) / 1000 + "s");
            }
            int nr = 0;
            for (Map.Entry<Node, HashMap<String, Object>> en: nodeInfo.entrySet()) {
                nr++;
                System.out.println("Starting routing " + nr + "/" + nodeInfo.size() + " (length: " + minLength / 1000 + "-" + maxLength / 1000 + "km, " + alternatives + " attempts)...");
                CandidateSelector cs = new DistPlSelector(en.getKey());
                start = System.currentTimeMillis();
                RouteLengthFinder rlf = new RouteLengthFinder(wb, en.getKey(), cs, minLength, maxLength, lambda, strictness, alternatives, g2);
                LinkedList<Path> paths = rlf.findRoutes();
                stop = System.currentTimeMillis();
                System.out.println("Routing finished " + nr + "/" + nodeInfo.size() + " (length: " + minLength / 1000 + "-" + maxLength / 1000 + "km, " + alternatives + " attempts)! Routing time: " + 1.0 * (stop - start) / 1000 + "s");

                HashMap<String, Object> map = en.getValue();
                if (!map.containsKey("tag_results")) map.put("tag_results", new JSONArray());
                JSONArray results = (JSONArray) map.get("tag_results");
                JSONObject result = new JSONObject();
                JSONObject configuration = new JSONObject();
                configuration.put("algorithm", "heuristic");
                configuration.put("reach", g2.getReach());
                configuration.put("minLength", minLength);
                configuration.put("maxLength", maxLength);
                configuration.put("strictness", strictness);
                configuration.put("lambda", lambda);
                configuration.put("alternatives", alternatives);
                JSONObject output = new JSONObject();
                output.put("time", (stop-start)/1000.);
                output.put("extractTime", rlf.extractionTime/1000.);
                output.put("forwardTime", rlf.forwardTime/1000.);
                output.put("backwardTimeAvg", rlf.backwardTime/(1000.*alternatives));
                JSONArray ps = new JSONArray();
                double bestScore = Double.MAX_VALUE;
                for (Path p : paths) {
                    double weight = p.getWeight(wb) / p.getLength();
                    double interference = lambda * p.getInterference(strictness) / p.getLength();
                    double score = weight + interference;
                    bestScore = Math.min(bestScore, score);
                    p.addTag("length", p.getLength());
                    p.addTag("weight", weight);
                    p.addTag("interf", interference);
                    p.addTag("score", score);
                    ps.add(p.toJSON());
                }
                output.put("bestScore", bestScore);
                output.put("paths", ps);
                result.put("configuration", configuration);
                result.put("output", output);
                results.add(result);
            }
            System.out.println("Writing routes to '" + out + "'...");
            start = System.currentTimeMillis();
            NodeWriter nw  = new NodeWriter(nodeInfo);
            JSONObject nodes = new JSONObject();
            nodes.put("nodes", nw.toJSON());
            JsonWriter jw = new JsonWriter(nodes);
            jw.write(out);
            stop = System.currentTimeMillis();
            System.out.println("Routes written! Writing time: " + 1.0 * (stop - start) / 1000 + "s");
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException|ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
}