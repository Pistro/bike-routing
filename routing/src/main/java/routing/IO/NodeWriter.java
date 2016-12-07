package routing.IO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.graph.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by piete on 22/11/2016.
 */
public class NodeWriter {
    private final HashMap<Node, HashMap<String, Object>> nodes;

    public NodeWriter() {
        this(new HashMap<>());
    }

    public NodeWriter(HashMap<Node, HashMap<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public void add(Node n) {
        add(n, new HashMap<>());
    }

    public void add(Node n, HashMap<String, Object> attrs) {
        if (nodes.containsKey(n)) nodes.get(n).putAll(attrs);
        else nodes.put(n, attrs);
    }

    public JSONArray toJSON() {
        JSONArray out = new JSONArray();
        for (Map.Entry<Node, HashMap<String, Object>> en: nodes.entrySet()) {
            JSONObject node = en.getKey().toJSON();
            for (Map.Entry<String, Object> en1: en.getValue().entrySet()) {
                if (en1.getKey().length()>=4 && en1.getKey().substring(0, 4).equals("tag_")) node.put(en1.getKey(), en1.getValue());
                else node.put("tag_" + en1.getKey(), en1.getValue());
            }
            out.add(node);
        }
        return out;
    }
}
