package routing.main.command;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.IO.XMLSPGraphReader;
import routing.IO.XMLSPGraphWriter;
import routing.algorithms.exact.LengthReachFinder;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.DefaultParameters;
import routing.main.Main;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piete on 6/06/2016.
 */
public class FindReach extends Command {
    private String hyperOut;
    private WeightBalancer wb;
    private double maxLength;
    private String hyperIn;
    private double reach;
    private int nrThreads;

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
        hyperOut = ap.getString("hyperOut");
        // Choice
        hyperIn = ap.getString("hyperIn", null);
        reach = ap.getDouble("reach", -1);
        if (hyperIn==null && reach==-1) throw new IllegalArgumentException("Either reach or hyperIn should be specified!");
        // Optional
        wb = new WeightBalancer(ap.getDouble("wFast", DefaultParameters.WFAST), ap.getDouble("wAttr", DefaultParameters.WATTR), ap.getDouble("wSafe", DefaultParameters.WSAFE));
        nrThreads = ap.getInt("threads", DefaultParameters.THREADS);
    }

    public void execute(Graph g) {
        try {
            long start, stop;
            SPGraph g2;
            if (hyperIn != null) {
                System.out.println("Reading hypergraph...");
                start = System.currentTimeMillis();
                XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                XMLSPGraphReader gr = new XMLSPGraphReader(g);
                xmlReader.setContentHandler(gr);
                xmlReader.parse(Main.convertToFileURL(hyperIn));
                g2 = gr.getSpGraph();
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph Read! Reading time: " + (stop-start)/1000. + "s");
            } else {
                System.out.println("Creating hypergraph...");
                start = System.currentTimeMillis();
                g2 = new SPGraph(g, reach, true, wb);
                stop = System.currentTimeMillis();
                System.out.println("Hypergraph created! Creation time: " + (stop-start)/1000. + "s");
            }
            System.out.println("Calculating reaches (up to " + maxLength/1000. + "km)...");
            start = System.currentTimeMillis();
            new LengthReachFinder(g, g2, wb, maxLength, nrThreads);
            stop = System.currentTimeMillis();
            System.out.println("Reaches calculated! Reach calculation time: " + (stop-start)/1000.0 + "s");
            System.out.println("Writing reaches...");
            XMLSPGraphWriter xmlGw = new XMLSPGraphWriter(g2, hyperOut);
            start = System.currentTimeMillis();
            xmlGw.write();
            stop = System.currentTimeMillis();
            System.out.println("Reaches written! Writing time: " + (stop-start)/1000. + "s");
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
