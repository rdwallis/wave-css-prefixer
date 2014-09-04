package com.wallissoftware.cssprefixer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class GwtCssPrefixer {

    private final static Set<String> prefixes = new HashSet<>();

    static {
        prefixes.add("-moz-");
        prefixes.add("-ms-");
        prefixes.add("-o-");
        prefixes.add("-webkit-");
        prefixes.add("-epub-");
    }



    public static void main(final String[] args) throws IOException, DocumentException {
        new GwtCssPrefixer(args[0]).execute();

    }

    private final String srcPath;
    private final Map<String, Set<String>> vendorMap = new HashMap<>();

    public GwtCssPrefixer(final String path) {
        this.srcPath = path;
        System.out.println("Processing: " + path);

    }

    private void execute() throws IOException, DocumentException {
        fetchVendorMap();
        final File folder = new File(srcPath);
        processFolder(folder);

    }

    @SuppressWarnings("unchecked")
    private void fetchVendorMap() throws IOException, DocumentException {
        final String rawHtml = IOUtils.toString(new URL(
                "http://peter.sh/experiments/vendor-prefixed-css-property-overview/"));
        final int startTable = rawHtml.indexOf("<table class=\"overview-table\">");
        final int endTable = rawHtml.indexOf("</table>", startTable) + "</table>".length() + 1;
        final String xml = rawHtml.substring(startTable, endTable).replace("&nbsp;", " ");
        final SAXReader reader = new SAXReader();
        final Document doc = reader.read(new StringReader(xml));

        final Element root = doc.getRootElement();

        for (final Iterator<Element> i = root.elementIterator(); i.hasNext();) {
            final Element rootElement = i.next();
            System.out.println(rootElement.getName());
            if (rootElement.getName().equals("tbody")) {
                for (final Iterator<Element> j = rootElement.elementIterator(); j.hasNext();) {
                    processRow(j.next());
                }
            }
        }

    }

    private boolean isPrefixed(final String input) {
        boolean isPrefixed = false;
        for (final String prefix: prefixes) {
            isPrefixed = isPrefixed || input.trim().startsWith(prefix);
        }
        return isPrefixed;
    }

    private void processCss(final File file) throws IOException {
        final List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        final StringBuffer buffer = new StringBuffer();
        for (final String line: lines) {

            if (!isPrefixed(line)) {

                if (line.contains(":")) {
                    final String prop = line.split(":")[0].trim();
                    if (vendorMap.containsKey(prop)) {
                        for (final String prefixProp: vendorMap.get(prop)) {
                            buffer.append(line.replace(prop, prefixProp)).append("\n");
                        }
                    }
                }
                buffer.append(line).append("\n");
            }
        }
        FileUtils.writeStringToFile(file, buffer.toString());
    }

    private void processFolder(final File folder) throws IOException {
        final File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            final File file = listOfFiles[i];

            if (file.isDirectory()) {
                processFolder(file);
            } else if (file.getPath().endsWith("ui.xml") || file.getPath().endsWith("css")) {
                processCss(file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRow(final Element row) {
        final List<String> vendorProps = new ArrayList<>();
        for (final Iterator<Element> i = row.elementIterator(); i.hasNext();) {
            final Element elem = i.next();
            vendorProps.add(elem.getStringValue().split(" ")[0]);
        }
        vendorProps.remove(vendorProps.size() - 1);

        String prop = null;
        final Set<String> vpSet = new HashSet<>();
        for (final String vProp: vendorProps) {
            if (!vProp.trim().isEmpty()) {
                if (isPrefixed(vProp)) {
                    vpSet.add(vProp);
                } else {
                    prop = vProp;
                }
            }
        }
        if (prop == null) {
            for (final String vp: vpSet) {
                prop = vp.substring(vp.substring(1).indexOf("-") + 1);
            }
        }
        if (prop != null) {
            vendorMap.put(prop, vpSet);
        }
    }

}
