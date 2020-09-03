package org.ml_methods_group.common.ast;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;

public  class srcmlGenerator extends TreeGenerator {

    private static final String SRCML_CMD = System.getProperty("gumtree.srcml.path", "srcml");

    private static final QName POSSTART = new  QName("http://www.srcML.org/srcML/position", "start", "pos");

    private static final QName POSEND = new  QName("http://www.srcML.org/srcML/position", "end", "pos");

    private LineReader lr;

    private Set<String> labeled = new HashSet<String>(
            Arrays.asList("specifier", "name", "comment", "literal", "operator"));

    private StringBuffer currentLabel;

    private TreeContext context;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        lr = new LineReader(r);
        String xml = getXml(lr);
        return getTreeContext(xml);
    }

    public TreeContext getTreeContext(String xml) {
        XMLInputFactory fact = XMLInputFactory.newInstance();
        context = new TreeContext();
        currentLabel = new StringBuffer();
        try {
            Stack<ITree> trees = new Stack<>();
            XMLEventReader r = fact.createXMLEventReader(new StringReader(xml));
            while (r.hasNext()) {
                XMLEvent ev = r.nextEvent();
                if (ev.isStartElement()) {
                    StartElement s = ev.asStartElement();
                    String typeLabel = s.getName().getLocalPart();
                    if (typeLabel.equals("position"))
                        setLength(trees.peek(), s);
                    else {
                        int type = typeLabel.hashCode();
                        ITree t = context.createTree(type, "", typeLabel);

                        if (trees.isEmpty()) {
                            context.setRoot(t);
                            t.setPos(0);
                        } else {
                            t.setParentAndUpdateChildren(trees.peek());
                            setPos(t, s);
                        }
                        trees.push(t);
                    }
                } else if (ev.isEndElement()) {
                    EndElement end = ev.asEndElement();
                    if (!end.getName().getLocalPart().equals("position")) {
                        if (isLabeled(trees))
                            trees.peek().setLabel(currentLabel.toString());
                        trees.pop();
                        currentLabel = new StringBuffer();
                    }
                } else if (ev.isCharacters()) {
                    Characters chars = ev.asCharacters();
                    if (!chars.isWhiteSpace() && isLabeled(trees))
                        currentLabel.append(chars.getData().trim());
                }
            }
            fixPos(context);
            context.validate();
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isLabeled(Stack<ITree> trees) {
        return labeled.contains(context.getTypeLabel(trees.peek().getType()));
    }

    private void fixPos(TreeContext ctx) {
        for (ITree t : ctx.getRoot().postOrder()) {
            if (!t.isLeaf()) {
                if (t.getPos() == ITree.NO_VALUE || t.getLength() == ITree.NO_VALUE) {
                    ITree firstChild = t.getChild(0);
                    t.setPos(firstChild.getPos());
                    if (t.getChildren().size() == 1)
                        t.setLength(firstChild.getLength());
                    else {
                        ITree lastChild = t.getChild(t.getChildren().size() - 1);
                        t.setLength(lastChild.getEndPos() - firstChild.getPos());
                    }
                }
            }
        }
    }

    private void setPos(ITree t, StartElement e) {
        if (e.getAttributeByName(POSSTART) != null) {
            String posStr =e.getAttributeByName(POSSTART).getValue();
            String[] chanks = posStr.split(":");
            int line = Integer.parseInt(chanks[0]);
            int column = Integer.parseInt(chanks[1]);
            t.setPos(lr.positionFor(line, column));
            setLength(t, e);
        }
    }

    private void setLength(ITree t, StartElement e) {
        if (t.getPos() == -1)
            return;
        if ( e.getAttributeByName(POSEND) != null) {
            String posStr =e.getAttributeByName(POSEND).getValue();
            String[] chanks = posStr.split(":");
            int line = Integer.parseInt(chanks[0]);
            int column = Integer.parseInt(chanks[1]);
            t.setLength(lr.positionFor(line, column) - t.getPos() + 1);
        }
    }

    public String getXml(Reader r) throws IOException {
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", "");
        FileWriter w = new FileWriter(f);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        while (line != null) {
            w.append(line);
            w.append(System.lineSeparator());
            line = br.readLine();
        }
        w.close();
        br.close();
        ProcessBuilder b = new ProcessBuilder(getArguments(f.getAbsolutePath()));
        b.directory(f.getParentFile());
        try {
            Process p = b.start();
            StringBuffer buf = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // TODO Why do we need to read and bufferize everything, when we could/should only use generateFromStream
            line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + "\n");
            p.waitFor();
            if (p.exitValue() != 0) throw new RuntimeException(buf.toString());
            r.close();
            String xml = buf.toString();
            return xml;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }

    public  String getLanguage(){return "C";};

    public String[] getArguments(String file) {
        return new String[]{SRCML_CMD, "-l", getLanguage(), "--position", file, "--tabs=1"};
    }
}
