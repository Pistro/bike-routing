package routing.main.command;

import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.algorithms.exact.LengthReachFinder;
import routing.algorithms.exact.ReachFinder;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.DefaultParameters;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pieter on 6/06/2016.
 */
public class FindReachFloyd extends Command {
    private String in;
    private String out;
    private WeightBalancer wb;

    public FindReachFloyd() {}

    public FindReachFloyd(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return false; }

    public String getName() {
        return "reachFloyd";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        in = Main.convertToFileURL(ap.getString("in"));
        wb = new WeightBalancer(ap.getDouble("wFast", DefaultParameters.WFAST), ap.getDouble("wAttr", DefaultParameters.WATTR), ap.getDouble("wSafe", DefaultParameters.WSAFE));
    }

    public void execute(Graph g) {
        System.out.println("Calculating all reaches (using the Floyd-Warshall algorithm)...");
        long start = System.currentTimeMillis();
        ReachFinder r = new ReachFinder(g, wb);
        long stop = System.currentTimeMillis();
        System.out.println("Reaches calculated! Reach calculation time: " + (stop-start)/1000. + "s");
        System.out.println("Preparing reaches for writing...");
        start = System.currentTimeMillis();
        HashMap<Long, HashMap<String, String>> reachesAttr = new HashMap<Long, HashMap<String, String>>();
        HashMap<Node, Double> reaches = r.getReaches();
        for (Map.Entry<Node, Double> en: reaches.entrySet()) {
            HashMap<String, String> tmp = new HashMap<String, String>();
            tmp.put("reach", Double.toString(Math.ceil(en.getValue()*100)/100.));
            reachesAttr.put(en.getKey().getId(), tmp);
        }
        stop = System.currentTimeMillis();
        System.out.println("Reaches prepared! Preparation time: " + (stop-start)/1000. + "s");
        System.out.println("Writing reaches...");
        start = System.currentTimeMillis();
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLGraphWriter xmlGw = new XMLGraphWriter(out);
            xmlGw.setNodeAttrUpdates(reachesAttr);
            xmlReader.setContentHandler(xmlGw);
            start = System.currentTimeMillis();
            xmlReader.parse(in);
        } catch (Exception e) {
            // XML parser exceptions and stuff. Should never occur...
        }
        stop = System.currentTimeMillis();
        System.out.println("Reaches written! Writing time: " + (stop-start)/1000. + "s");
    }
}
