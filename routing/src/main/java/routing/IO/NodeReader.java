package routing.IO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import routing.graph.Graph;
import routing.graph.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by piete on 22/11/2016.
 */
public class NodeReader {
    private final Graph g;
    public final HashMap<Node, HashMap<String, Object>> nodes = new HashMap<>();

    public NodeReader(Graph g, JSONArray ar) {
        this.g = g;
        for (Object o: ar) {
            JSONObject jo = (JSONObject) o;
            Node n = g.getNode((Long) jo.get("id"));
            HashMap<String, Object> map = new HashMap<>();
            for (Object o1: jo.entrySet()) {
                Map.Entry<String, Object> en = (Map.Entry<String, Object>) o1;
                if (en.getKey().length()>=4 && en.getKey().substring(0, 4).equals("tag_")) map.put(en.getKey(), en.getValue());
            }
            nodes.put(n, map);
        }
    }

}
