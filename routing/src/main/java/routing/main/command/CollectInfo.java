package routing.main.command;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.XMLSPGraphReader;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piete on 27/12/2016.
 */
public class CollectInfo extends Command {
    private WeightBalancer wbReach;
    private double reach;
    private String hyperIn;

    public CollectInfo() {}

    public CollectInfo(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "info";
    }

    protected void initialize(ArgParser ap) {
        // Optional arguments
        hyperIn = ap.getString("hyperIn", null);
        reach = ap.getDouble("reach", -1);
        wbReach = new WeightBalancer(ap.getDouble("wbFast", 0.5), ap.getDouble("wbAttr", 0.25), ap.getDouble("wbSafe", 0.25));
    }

    public void execute(Graph g) {
        try {
            long start, stop;
            if (hyperIn!=null) {
                System.out.println("Reading hypergraph...");
                start = System.currentTimeMillis();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                XMLSPGraphReader gr = new XMLSPGraphReader(g);
                xmlReader.setContentHandler(gr);
                xmlReader.parse(Main.convertToFileURL(hyperIn));
                g = gr.getSpGraph();
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph Read! Reading time: " + (stop - start) / 1000. + "s");
            } else if (reach != -1) {
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g = new SPGraph(g, reach, true, wbReach);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + (stop - start) / 1000. + "s");
            }
            System.out.println("# vertices: " + g.getOrder());
            System.out.println("# edges: " + g.getSize());
            System.out.println("reaches (" + nrReaches(g)  + "/" + g.getOrder() + "): " + arrayToString(getReachBins(g)));
        } catch (ParserConfigurationException |IOException |SAXException e) {
            e.printStackTrace();
        }
    }

    private int nrReaches(Graph g) {
        int reachCount = 0;
        for (Node n: g.getNodes().values()) if (n.hasReach()) reachCount++;
        return reachCount;
    }

    private int [] getReachBins(Graph g) {
        HashMap<Integer, Integer> reachBins = new HashMap<>();
        for (Node n: g.getNodes().values()) {
            if (n.hasReach()) {
                int nBin = (int) (n.getReach() / 100);
                Integer nBinCount = reachBins.get(nBin);
                if (nBinCount == null) nBinCount = 0;
                reachBins.put(nBin, nBinCount + 1);
            }
        }
        int nrBins = 0;
        for (Integer binNr: reachBins.keySet()) nrBins = Math.max(nrBins, binNr+1);
        int [] reaches = new int[nrBins];
        for (Map.Entry<Integer, Integer> en: reachBins.entrySet()) reaches[en.getKey()] = en.getValue();
        return reaches;
    }

    private String arrayToString(int [] ar) {
        String out = "[";
        for (int i: ar) {
            if (out.length()==1) out += i;
            else out += ", " + i;
        }
        return out + "]";
    }
}
