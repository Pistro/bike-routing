package routing.IO;

/**
 * Created by pieter on 27/02/2016.
 */
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.*;

public class XMLWriter {
    static String indentation = "";
    private PrintWriter pw;
    private String lastLine = null;
    private boolean opened = false;

    public XMLWriter(String outputLoc) throws IOException {
        pw = new PrintWriter(new FileWriter(outputLoc));
    }

    public void startDocument() {
        lastLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    }

    public void endDocument() {
        pw.println(lastLine);
        pw.close();
    }

    public void startElement(String qualifiedName, Attributes attributes) {
        pw.println(lastLine);
        lastLine = indentation + "<" + qualifiedName;
        indentation += "    ";

        if (attributes != null) {
            int nrAtt = attributes.getLength();
            for (int i = 0; i < nrAtt; i++){
                lastLine += ' ' +  StringEscapeUtils.escapeXml10(attributes.getQName(i)) + "=\"" + StringEscapeUtils.escapeXml10(attributes.getValue(i)) + '"';
            }
        }
        lastLine += '>';
        opened = true;
    }

    public void startElement(String qualifiedName, HashMap<String, String> attributes) {
        pw.println(lastLine);
        lastLine = indentation + "<" + qualifiedName;
        indentation += "    ";

        if (attributes != null) {
            for (Map.Entry<String, String> en : attributes.entrySet()){
                lastLine += ' ' +  StringEscapeUtils.escapeXml10(en.getKey()) + "=\"" + StringEscapeUtils.escapeXml10(en.getValue()) + '"';
            }
        }
        lastLine += '>';
        opened = true;
    }

    public void endElement(String qualifiedName) {
        indentation = indentation.substring(0, indentation.length() - 4) ;
        if (opened) {
            lastLine = lastLine.substring(0, lastLine.length()-1) + "/>";
            opened = false;
        } else {
            pw.println(lastLine);
            lastLine = indentation + "</" + qualifiedName + '>';
        }
    }
}