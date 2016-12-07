package routing.main;

import org.json.simple.JSONArray;

import java.io.*;
import javax.xml.parsers.SAXParserFactory;

import org.json.simple.JSONObject;
import org.xml.sax.XMLReader;
import routing.IO.JsonWriter;
import routing.IO.NodeWriter;
import routing.IO.XMLGraphReader;
import routing.graph.*;
import routing.main.command.*;

public class Main {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Argument syntax: methodName -in inputPath -optionName1 optionValue1 -optionName2 optionValue2");
        }
        System.out.println("Parsing input...");
        ArgParser a = new ArgParser(args);
        String inPath;
        try {
            inPath = a.getString("in");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("An input file is required. Pass an input file by specifying the '-in' option");
        }
        Graph g = new Graph();
        Command [] commands = {new FindLength(), new FindLengthBatch(),
                                new FindLCC(), new Contract(), new FindReach(), new FindReachFloyd(),
                                new FindLengthE(), new SelectNodes(), new FindLengthEBatch() };
        Command c = null;
        for (Command com: commands) {
            if (com.getName().equals(a.getMethod())) {
                c = com.getClass().getDeclaredConstructor(ArgParser.class).newInstance(a);
                break;
            }
        }
        if (c == null) {
            String allCommands = "";
            for (Command com: commands) {
                if (allCommands.equals("")) allCommands = com.getName();
                else allCommands += ", " + com.getName();
            }
            throw new IllegalArgumentException("Unknown command specified. Valid commands are: " + allCommands);
        }
        System.out.println("Input succesfully parsed!");
        // Prepare the graph reader
        System.out.println("Reading graph...");
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        XMLGraphReader gr = new XMLGraphReader(g);
        if (!c.loadNodes()) gr.setDynamicNodes(true);
        xmlReader.setContentHandler(gr);
        long start = System.currentTimeMillis();
        xmlReader.parse(convertToFileURL(inPath));
        long stop = System.currentTimeMillis();
        System.out.println("Graph read! Read time: " + (stop - start) / 1000.0 + "s");
        c.execute(g);
    }

    private static void printReachDistribution(Graph g, double maxReach) {
        double precision = 500;
        int [] reachCounts = new int[(int) (maxReach/precision)];
        int larger = 0;
        for (Node n: g.getNodes().values()) {
            int pos = n.hasReach()? (int) (n.getReach()/precision) : Integer.MAX_VALUE;
            if (pos<=reachCounts.length) reachCounts[pos]++;
            else larger++;
        }
        for (int i = 0; i<reachCounts.length; i++) {
            System.out.println(precision*i + "-" + precision*(i+1) + ": " + reachCounts[i]);
        }
        System.out.println("Other: " + larger);
    }

    private static void writeNodes(Graph g, String outPath) throws IOException {
        int ndCntr = 0;
        JSONObject out = new JSONObject();
        JSONArray nodes = new JSONArray();
        out.put("nodes", nodes);
        for (Node n : g.getNodes().values()) {
            ndCntr++;
            if (ndCntr%100==0) nodes.add(n.toJSON());
        }
        new JsonWriter(out).write(outPath);
    }

    public static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    public static String printMatrix(double [] [] m, String name) {
        String out = name + " = [";
        for (int i = 0; i<m.length; i++) {
            for (int j = 0; j<m[i].length-1; j++) {
                out += m[i][j] + ", ";
            }
            out += m[i][m[i].length-1] + ((i == m.length-1)? "];" : ";\n");
        }
        return out;
    }

    public static String printMatrix(int [] [] m, String name) {
        String out = name + " = [";
        for (int i = 0; i<m.length; i++) {
            for (int j = 0; j<m[i].length-1; j++) {
                out += m[i][j] + ", ";
            }
            out += m[i][m[i].length-1] + ((i == m.length-1)? "];" : ";\n");
        }
        return out;
    }
}