package routing.main.command;

import org.xml.sax.XMLReader;
import routing.IO.XMLGraphWriter;
import routing.IO.XMLSPGraphWriter;
import routing.algorithms.exact.ComponentDiscovery;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.SPGraph;
import routing.graph.weights.WeightBalancer;
import routing.main.ArgParser;
import routing.main.Main;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by piete on 15/12/2016.
 */
public class FindSPGraph extends Command{
    private double reach;
    private double wAttr;
    private double wSafe;
    private double wFast;
    private boolean bi;
    private String out;
    public FindSPGraph() {}

    public FindSPGraph(ArgParser a) {
        super(a);
    }

    public String getName() {
        return "findSPGraph";
    }

    protected void initialize(ArgParser ap) {
        out = ap.getString("out");
        reach = ap.getDouble("reach");
        // Optionals
        wAttr = ap.getDouble("wAttr", 1.0/6);
        wSafe = ap.getDouble("wSafe", 1.0/6);
        wFast = ap.getDouble("wSafe", 1.0/3);
        bi = ap.getInt("bi", 1) != 0;
    }

    public boolean loadNodes() { return false; }

    public void execute(Graph g) {
        System.out.println("Extracting SPGraph (reach: " + reach + ")...");
        long start = System.currentTimeMillis();
        SPGraph g2 = new SPGraph(g, reach, bi, new WeightBalancer(wFast, wAttr, wSafe));
        long stop = System.currentTimeMillis();
        System.out.println("SPGraph extracted! Extraction time: " + 1.0 * (stop - start) / 1000 + "s");
        System.out.println("Original graph order: " + g.getNodes().size() + ", SPGraph order: " + g2.getNodes().size() + ", relative: " + ((float) g2.getNodes().size() / g.getNodes().size()));
        try {
            System.out.println("Writing SPGraph to '" + out + "'...");
            new XMLSPGraphWriter(g2, out).write();
            System.out.println("SPGraph written. Write time: " + 1.0 * (stop - start) / 1000 + "s");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
