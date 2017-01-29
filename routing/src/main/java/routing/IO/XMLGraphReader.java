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
import routing.graph.*;

/**
 *
 * @author Pieter
 */
public class XMLGraphReader extends DefaultHandler {
    private final Graph graph = new Graph();
    private boolean keep = false;
    private final HashMap<String, String> curAttrs = new HashMap<>();
    private final HashMap<String, String> curTags = new HashMap<>();
    private final LinkedList<Integer> segments = new LinkedList<>();
    private boolean dynamicNodes = false;
    private boolean fullEdges = false;
    private double accuracy = -1;

    public Graph getGraph() { return graph; }
    public void setDynamicNodes(boolean b) { this.dynamicNodes = b; }
    public void setFullEdges(boolean b) { this.fullEdges = b; }
    public void setAccuracy(double d) { this.accuracy = d; }
    
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
                double length = Double.parseDouble(curTags.get("length"));
                if (accuracy>0) length = Math.max(1, Math.ceil(length/accuracy))*accuracy;
                double score_safe_lin = Double.parseDouble(curTags.get("score_safe_lin"));
                double score_safe_const = Double.parseDouble(curTags.get("score_safe_const"));
                double score_safe = score_safe_lin*length+score_safe_const;
                double score_attr_lin = Double.parseDouble(curTags.get("score_attr_lin"));
                double score_attr_const = Double.parseDouble(curTags.get("score_attr_const"));
                double score_attr = score_attr_lin*length+score_attr_const;
                double score_fast_lin = Double.parseDouble(curTags.get("score_fast_lin"));
                double score_fast_const = Double.parseDouble(curTags.get("score_fast_const"));
                double score_fast = score_fast_lin*length+score_fast_const;
                Node start, stop;
                if (!dynamicNodes) {
                    start = graph.getNode(Long.parseLong(curTags.get("start_node")));
                    stop = graph.getNode(Long.parseLong(curTags.get("end_node")));
                } else {
                    start = graph.addNode(Long.parseLong(curTags.get("start_node")), 0, 0);
                    stop = graph.addNode(Long.parseLong(curTags.get("end_node")), 0, 0);
                }
                if (curTags.get("bicycle_oneway").equals("-1")) {
                    Node tmp = stop;
                    stop = start;
                    start = tmp;
                }
                Edge e1;
                if (fullEdges) e1 = new FullEdge(id, start, stop, length, score_fast_const, score_fast_lin, score_attr_const, score_attr_lin, score_safe_const, score_safe_lin);
                else e1 = new SimpleEdge(id, start, stop, length, score_fast, score_attr, score_safe);
                int nrSegments = segments.size();
                if (nrSegments != 0) {
                    e1.shadow = new int[nrSegments];
                    for (int i = 0; i<nrSegments; i++) e1.shadow[i] = segments.get(i);
                } else {
                    e1.shadow = new int[1];
                    e1.shadow[0] = id;
                }
                if (curTags.get("bicycle_oneway").equals("0")) {
                    Edge e2;
                    if (fullEdges) e2 = new FullEdge(id, stop, start, length, score_fast_const, score_fast_lin, score_attr_const, score_attr_lin, score_safe_const, score_safe_lin);
                    else e2 = new SimpleEdge(id, stop, start, length, score_fast, score_attr, score_safe);
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
