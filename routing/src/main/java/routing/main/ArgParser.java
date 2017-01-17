package routing.main;

import java.util.HashMap;

/**
 * Created by Pieter on 4/06/2016.
 */
public class ArgParser {
    private HashMap<String, String> options = new HashMap<String, String>();
    private String method;
    public ArgParser(String[] args) {
        if (args.length==0) throw new IllegalArgumentException("No method name passed!");
        method = args[0];
        for (int i = 1; i<args.length; i++) {
            if (args[i].charAt(0) == '-') {
                String key = args[i].substring(1);
                if (i==args.length || args[i+1].charAt(0)=='-') {
                    options.put(key, null);
                } else {
                    String value = args[++i];
                    options.put(key, value);
                }
            } else {
                throw new IllegalArgumentException("Incorrect syntax, options should be like: -optionName optionValue");
            }
        }
    }
    public String getMethod() {
        return method;
    }
    public double getDouble(String key, double def) {
        if (options.containsKey(key)) return Double.parseDouble(options.get(key));
        else return def;
    }
    public double getDouble(String key) {
        if (options.containsKey(key)) return Double.parseDouble(options.get(key));
        else throw new IllegalArgumentException("Required option missing: " + key);
    }
    public long getLong(String key, long def) {
        if (options.containsKey(key)) return Long.parseLong(options.get(key));
        else return def;
    }
    public long getLong(String key) {
        if (options.containsKey(key)) return Long.parseLong(options.get(key));
        else throw new IllegalArgumentException("Required option missing: " + key);
    }
    public int getInt(String key, int def) {
        if (options.containsKey(key)) return Integer.parseInt(options.get(key));
        else return def;
    }
    public int getInt(String key) {
        if (options.containsKey(key)) return Integer.parseInt(options.get(key));
        else throw new IllegalArgumentException("Required option missing: " + key);
    }
    public String getString(String key, String def) {
        if (options.containsKey(key)) return options.get(key);
        else return def;
    }
    public String getString(String key) {
        if (options.containsKey(key)) return options.get(key);
        else throw new IllegalArgumentException("Required option missing: " + key);
    }
}
