package routing.IO;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import routing.graph.*;
import routing.graph.weights.WeightBalancer;

import java.io.IOException;
import java.util.*;

/**
 * Created by Pieter on 14/12/2016.
 */
public class XMLSPGraphReader extends DefaultHandler {
    private final Graph graph;
    private final SPGraph spGraph = new SPGraph();
    private boolean keep = false;
    private final HashMap<String, String> curAttrs = new HashMap<String, String>();
    private final HashMap<String, String> curTags = new HashMap<String, String>();

    public XMLSPGraphReader(Graph g) {
        this.graph = g;
    }
    public SPGraph getSpGraph() { return spGraph; }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
        if (qName.equals("node")) {
            curAttrs.clear();
            curTags.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i < nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (qName.equals("way")) {
            curAttrs.clear();
            curTags.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i < nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (qName.equals("osm")) {
            HashMap<String, String> osmAttrs = new HashMap<>();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i < nrAttrs; i++) osmAttrs.put(attrs.getQName(i), attrs.getValue(i));
            spGraph.setBi(Integer.parseInt(osmAttrs.get("bi"))!=0);
            spGraph.setReach(Double.parseDouble(osmAttrs.get("reach")));
            spGraph.setWeightBalancer(new WeightBalancer(Double.parseDouble(osmAttrs.get("wFast")), Double.parseDouble(osmAttrs.get("wAttr")), Double.parseDouble(osmAttrs.get("wSafe"))));
        } else if (keep) {
            if (qName.equals("tag")) curTags.put(attrs.getValue("k"), attrs.getValue("v"));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (keep) {
            if (qName.equals("node")) {
                keep = false;
                Node startNode = graph.getNode(Long.parseLong(curAttrs.get("start_id")));
                Node endNode = graph.getNode(Long.parseLong(curAttrs.get("stop_id")));
                Node n = spGraph.addNodePair(Long.parseLong(curAttrs.get("id")), startNode, endNode);
                if (curTags.containsKey("reach")) n.setReach(Double.parseDouble(curTags.get("reach")));
            } else if (qName.equals("way")) {
                keep = false;
                int id = Integer.parseInt(curAttrs.get("id"));
                Node start = spGraph.getNode(Long.parseLong(curTags.get("start_node")));
                Node stop = spGraph.getNode(Long.parseLong(curTags.get("end_node")));
                for (Edge e: ((SPGraph.NodePair) start).e.getOutEdges()) {
                    if (e.getId() == id) {
                        new SimpleEdge(e, start, stop);
                    }
                }
            }
        }
    }
}