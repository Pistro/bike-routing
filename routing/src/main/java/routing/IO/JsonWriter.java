package routing.IO;

import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by piete on 13/12/2015.
 */
public class JsonWriter {
    JSONObject j;
    public JsonWriter(JSONObject j) {
        this.j = j;
    }
    public void write(String path) {
        try {
            FileWriter file = new FileWriter(path);
            file.write(j.toJSONString());
            file.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
