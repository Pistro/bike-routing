package routing.main.command;

import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.algorithms.exact.LengthReachFinder;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piete on 6/06/2016.
 */
public class FindReach extends Command {
    private String in;
    private String out;
    private WeightBalancer wb;
    private double maxLength;

    public FindReach() {}

    public FindReach(ArgParser a) {
        super(a);
    }

    public boolean loadNodes() { return false; }

    public String getName() {
        return "reach";
    }

    protected void initialize(ArgParser ap) {
        maxLength = ap.getDouble("maxLength");
        out = ap.getString("out");
        in = Main.convertToFileURL(ap.getString("in"));
        wb = new WeightBalancer(ap.getDouble("wFast", 0.5), ap.getDouble("wAttr", 0.25), ap.getDouble("wSafe", 0.25));
    }

    public void execute(Graph g) {
        System.out.println("Calculating reaches (to nodes within " + maxLength/1000 + "km)...");
        long start = System.currentTimeMillis();
        LengthReachFinder r = new LengthReachFinder(g, wb, maxLength);
        long stop = System.currentTimeMillis();
        System.out.println("Reaches calculated! Reach calculation time: " + (stop-start)/1000.0 + "s");
        System.out.println("Preparing reaches for writing...");
        start = System.currentTimeMillis();
        HashMap<Long, HashMap<String, String>> reachesAttr = new HashMap<Long, HashMap<String, String>>();
        HashMap<Node, Double> reaches = r.getReaches();
        for (Map.Entry<Node, Double> en: reaches.entrySet()) {
            HashMap<String, String> tmp = new HashMap<String, String>();
            tmp.put("reach", Double.toString(Math.round(en.getValue()*100)/100.0));
            reachesAttr.put(en.getKey().getId(), tmp);
        }
        stop = System.currentTimeMillis();
        System.out.println("Reaches prepared! Preparation time: " + (stop-start)/1000.0 + "s");
        System.out.println("Writing reaches...");
        start = System.currentTimeMillis();
        try {
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLGraphWriter xmlGw = new XMLGraphWriter(out);
            xmlGw.setNodeTagUpdates(reachesAttr);
            xmlReader.setContentHandler(xmlGw);
            start = System.currentTimeMillis();
            xmlReader.parse(in);
        } catch (Exception e) {
            // XML parser exceptions and stuff. Should never occur...
        }
        stop = System.currentTimeMillis();
        System.out.println("Reaches written! Writing time: " + (stop-start)/1000.0 + "s");
        r.apply();
    }
}
