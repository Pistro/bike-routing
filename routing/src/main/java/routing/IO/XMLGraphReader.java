/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.IO;

import java.util.HashMap;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import routing.graph.Edge;
import routing.graph.Graph;
import routing.graph.Node;
import routing.graph.SimpleNode;

/**
 *
 * @author piete
 */
public class XMLGraphReader extends DefaultHandler {
    private final Graph graph = new Graph();
    private boolean keep = false;
    private final HashMap<String, String> curAttrs = new HashMap<String, String>();
    private final HashMap<String, String> curTags = new HashMap<String, String>();
    private final LinkedList<Integer> segments = new LinkedList<Integer>();
    private boolean dynamicNodes = false;

    public Graph getGraph() { return graph; }
    public void setDynamicNodes(boolean b) { this.dynamicNodes = b; }
    
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
        if (qName.equals("node") && !dynamicNodes) {
            curAttrs.clear();
            curTags.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (qName.equals("way")) {
            curAttrs.clear();
            curTags.clear();
            segments.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (keep) {
            if (qName.equals("tag")) curTags.put(attrs.getValue("k"), attrs.getValue("v"));
            else if (qName.equals("wy")) segments.add(Integer.parseInt(attrs.getValue("ref")));
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (keep) {
            if (qName.equals("node") && !dynamicNodes) {
                keep = false;
                Node n = new SimpleNode(Long.parseLong(curAttrs.get("id")), Double.parseDouble(curAttrs.get("lat")), Double.parseDouble(curAttrs.get("lon")));
                if (curTags.containsKey("reach")) n.setReach(Double.parseDouble(curTags.get("reach")));
                graph.addNode(n);
            } else if (qName.equals("way")) {
                keep = false;
                int id = Integer.parseInt(curAttrs.get("id"));
                double score_safe = Double.parseDouble(curTags.get("score_safe"));
                double score_attr = Double.parseDouble(curTags.get("score_attr"));
                double score_fast = Double.parseDouble(curTags.get("score_fast"));
                double height_dif = Double.parseDouble(curTags.get("height_dif"));
                double length = Double.parseDouble(curTags.get("length"));
                Node start, stop;
                if (!dynamicNodes) {
                    start = graph.getNode(Long.parseLong(curTags.get("start_node")));
                    stop = graph.getNode(Long.parseLong(curTags.get("end_node")));
                } else {
                    start = graph.addNode(Long.parseLong(curTags.get("start_node")), 0, 0);
                    stop = graph.addNode(Long.parseLong(curTags.get("end_node")), 0, 0);
                }
                Edge e1 = new Edge(start, stop, length, height_dif, score_fast, score_attr, score_safe);
                int nrSegments = segments.size();
                if (nrSegments != 0) {
                    e1.shadow = new int[nrSegments];
                    for (int i = 0; i<nrSegments; i++) e1.shadow[i] = segments.get(i);
                } else {
                    e1.shadow = new int[1];
                    e1.shadow[0] = id;
                }
                if (!(curTags.containsKey("bicycle_oneway") && curTags.get("bicycle_oneway").equals("yes"))) {
                    Edge e2 = new Edge(stop, start, length, height_dif, score_fast, score_attr, score_safe);
                    if (nrSegments != 0) {
                        e2.shadow = new int[nrSegments];
                        for (int i = 0; i<nrSegments; i++) e2.shadow[i] = segments.get(nrSegments-1-i);
                    } else {
                        e2.shadow = new int[1];
                        e2.shadow[0] = id;
                    }
                }
            }
        }
    }
}
