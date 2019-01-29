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
    private final LinkedList<Long> curNodeIds = new LinkedList<>();
    private boolean dynamicNodes = false;
    HashMap<Long, Node> allNodes = new HashMap<>();

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
            curNodeIds.clear();
            int nrAttrs = attrs.getLength();
            for (int i = 0; i<nrAttrs; i++) curAttrs.put(attrs.getQName(i), attrs.getValue(i));
            keep = true;
        } else if (keep) {
            if (qName.equals("tag")) curTags.put(attrs.getValue("k"), attrs.getValue("v"));
            else if (qName.equals("nd")) curNodeIds.add(Long.parseLong(attrs.getValue("ref")));
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (keep) {
            if (qName.equals("node") && !dynamicNodes) {
                keep = false;
                Node n = new SimpleNode(Long.parseLong(curAttrs.get("id")), Double.parseDouble(curAttrs.get("lat")), Double.parseDouble(curAttrs.get("lon")));
                if (curTags.containsKey("reach")) n.setReach(Double.parseDouble(curTags.get("reach")));
                allNodes.put(n.getId(), n);
            } else if (qName.equals("way")) {
                keep = false;
                if (curTags.containsKey("bicycle_oneway")) {
                    int id = Integer.parseInt(curAttrs.get("id"));
                    double length = Double.parseDouble(curTags.get("length"));
                    double score_attr = Double.parseDouble(curTags.get("score_attr"));
                    double score_fast = Double.parseDouble(curTags.get("score_fast"));
                    double score_safe = Double.parseDouble(curTags.get("score_safe"));
                    Node start, stop;
                    if (!dynamicNodes) {
                        start = allNodes.get(curNodeIds.getFirst());
                        if (start == null) throw new IllegalArgumentException("Way " + id + " refers to node " + curNodeIds.getFirst() + ", which is not present");
                        stop = allNodes.get(curNodeIds.getLast());
                        if (stop == null) throw new IllegalArgumentException("Way " + id + " refers to node " + curNodeIds.getLast() + ", which is not present");
                    } else {
                        start = new SimpleNode(curNodeIds.getFirst(), 0, 0);
                        stop = new SimpleNode(curNodeIds.getLast(), 0, 0);
                    }
                    start = graph.addNode(start);
                    stop = graph.addNode(stop);
                    if (curTags.get("bicycle_oneway").equals("-1")) {
                        Node tmp = stop;
                        stop = start;
                        start = tmp;
                    }
                    Edge e1 = new Edge(id, start, stop, length, score_fast, score_attr, score_safe);
                    if (!dynamicNodes) {
                        e1.intermediateNodes = new Node[curNodeIds.size() - 2];
                        int pos = 0;
                        for (Long l : curNodeIds) {
                            if (pos > 0 && pos < curNodeIds.size() - 1) {
                                Node n = allNodes.get(l);
                                if (n == null) throw new IllegalArgumentException("Way " + id + " refers to node " + l + ", which is not present");
                                e1.intermediateNodes[pos - 1] = n;
                            }
                            pos++;
                        }
                    }
                    if (curTags.get("bicycle_oneway").equals("0")) {
                        Edge e2 = new Edge(id, stop, start, length, score_fast, score_attr, score_safe);
                        if (!dynamicNodes) {
                            e2.intermediateNodes = new Node[e1.intermediateNodes.length];
                            for (int i = 0; i < e2.intermediateNodes.length; i++)
                                e2.intermediateNodes[i] = e1.intermediateNodes[e1.intermediateNodes.length - 1 - i];
                        }
                    }
                }
            }
        }
    }
}
