
package org.ml_methods_group;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.srcml.SrcmlCTreeGenerator;
import org.ml_methods_group.common.ast.srcmlGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.io.TreeIoUtils;
import org.ml_methods_group.common.*;
import org.ml_methods_group.common.ast.NodeType;
import org.ml_methods_group.common.ast.ASTUtils;
import org.ml_methods_group.common.ast.changes.BasicChangeGenerator;
import org.ml_methods_group.common.ast.changes.ChangeGenerator;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.common.ast.editactions.EditActionStore;
import org.ml_methods_group.common.ast.editactions.EditActions;
import org.ml_methods_group.common.ast.editactions.ActionContext;
import org.ml_methods_group.common.ast.generation.ASTGenerator;
import org.ml_methods_group.common.ast.generation.BasicASTGenerator;
import org.ml_methods_group.common.ast.generation.CachedASTGenerator;
import org.ml_methods_group.common.ast.normalization.BasicASTNormalizer;
import org.ml_methods_group.common.ast.normalization.NamesASTNormalizer;
import org.ml_methods_group.common.extractors.ChangesExtractor;
import org.ml_methods_group.common.metrics.functions.HeuristicChangesBasedDistanceFunction;
import org.ml_methods_group.common.metrics.selectors.ClosestPairSelector;
import org.ml_methods_group.common.preparation.Unifier;
import org.ml_methods_group.common.preparation.basic.BasicUnifier;
import org.ml_methods_group.common.preparation.basic.MinValuePicker;
import org.ml_methods_group.common.serialization.ProtobufSerializationUtils;
import org.ml_methods_group.evaluation.approaches.clustering.ClusteringAlgorithm;
import org.ml_methods_group.parsing.ParsingUtils;
import org.ml_methods_group.testing.extractors.CachedFeaturesExtractor;
import org.ml_methods_group.common.ast.matches.testMatcher;
import org.ml_methods_group.ApplicationMethods;
import org.ml_methods_group.common.ast.srcmlGenerator;
import org.ml_methods_group.common.ast.suggestionItem;
import org.ml_methods_group.common.ast.suggestion;
import org.ml_methods_group.common.ast.seekItem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ApplicationSuggest extends ApplicationMethods {

    public static void LaseLookLike(Path pathToDataset1, Path pathToListFile1, Path pathToDataset2,
            Path pathToListFile2, Path pathToMatrix, String version, String verbose, Integer minCountOfMarkers, Boolean UseAnyNode) throws IOException {

        System.out.println("Markers: " + minCountOfMarkers);    
        System.out.println("Use any node: " + UseAnyNode);    

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToMatrix.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }
        // first dataset - test (bad files only)
        List<String> defects1 = Files.readAllLines(pathToListFile1);
        List<String> defectFiles1 = new ArrayList<String>();

        String badFolderName1 = pathToDataset1.toString() + "\\bad";

        try {

            List<String> result1 = Files.walk(Paths.get(badFolderName1)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result1) {
                boolean useFile = false;

                for (String defect : defects1) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles1.add(fName);
                }
            }

            // second dataset - template ( bad + good files)
            List<String> defects2 = Files.readAllLines(pathToListFile2);

            List<String> defectFiles2 = new ArrayList<String>();

            String badFolderName2 = pathToDataset2.toString() + "\\bad";
            String goodFolderName2 = pathToDataset2.toString() + "\\good";
            String astFolderName = pathToMatrix.toString() + "\\ast";

            List<String> result2 = Files.walk(Paths.get(badFolderName2)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result2) {
                boolean useFile = false;

                for (String defect : defects2) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles2.add(fName);
                }
            }

            // defect files is a collection of bad files
            if (defectFiles1.size() > 0 && defectFiles2.size() > 0) {

                // collect common actions for cluster here
                double[][] weightMatrix = new double[defectFiles2.size()][defectFiles1.size()];
                int[] sizes = new int[defectFiles2.size()];

                ASTGenerator generator = null;

                if (version.toLowerCase().equals("abstract")) {
                    generator = new CachedASTGenerator(new NamesASTNormalizer());
                } else {
                    generator = new CachedASTGenerator(new BasicASTNormalizer());
                }

                for (int i = 0; i < defectFiles2.size(); i++) {

                    // get template defects from second dataset
                    String defectB = defectFiles2.get(i);
                    String defectB_Name = "";

                    for (String defect : defects2) {

                        if (defectB.contains("\\" + defect + "\\")) {
                            defectB_Name = defect;
                            break;
                        }
                    }

                    TreeContext dstB = null;
                    List<Action> actB = null;
                    // List<String> seekCode= new ArrayList<String>();
                    Map<String, seekItem> seekCode = new HashMap<String, seekItem>();
                    String emuCode = "";

                    File actionsFile = new File(astFolderName + "//" + defectB_Name + ".ES");
                    File seekFile = new File(astFolderName + "//" + defectB_Name + ".seek");

                    try {
                        var fromCode = Files.readString(Paths.get(defectB));
                        var toCode = Files.readString(Paths.get(defectB.replace(badFolderName2, goodFolderName2)));
                        if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                            var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                            var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                            TreeContext srcB = null;

                            srcB = generator.buildTreeContext(fromSolution);
                            dstB = generator.buildTreeContext(toSolution);

                            Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                            // System.out.println("Compare trees");
                            try {
                                matcherAst.match();
                            } catch (NullPointerException e) {
                                System.out.println(e.getMessage());
                            }

                            ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(), dstB.getRoot(),
                                    matcherAst.getMappings());
                            try {
                                actionGenerator.generate();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                e.printStackTrace();
                            }

                            actB = actionGenerator.getActions();

                            if (actB != null && actB.size() > 0) {
                                seekCode.clear();
                                emuCode = "";

                                for (Action action : actB) {

                                    ITree actNode = null;
                                    String actString = action.getName();
                                    String seekString = "";

                                    if (action.getName() == "UPD") {
                                        Update u = (Update) action;
                                        actNode = u.getNode();
                                        actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                        if (UseAnyNode || actNode.hasLabel())
                                            seekString += ActionContext.GetContextPath(action, false, srcB);

                                        actString += " change to " + u.getValue();
                                    }

                                    if (action.getName() == "MOV" || action.getName() == "INS") {
                                        Addition ad = (Addition) action;
                                        actNode = ad.getParent();
                                        actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                        if (UseAnyNode ||actNode.hasLabel())
                                            seekString += ActionContext.GetContextPath(action, false, srcB);

                                    }

                                    if (action.getName() == "DEL") {
                                        Delete d = (Delete) action;
                                        actNode = d.getNode();
                                        actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                        if (UseAnyNode || actNode.hasLabel())
                                            seekString += ActionContext.GetContextPath(action, false, srcB);

                                    }

                                    if (verbose.equals("yes")) {
                                        if (seekString.length() > 0) {
                                            ITree nfa = ActionContext.GetContextRoot(action);

                                            if (nfa != null && nfa.getLength() > 0) {
                                                actString += "\r\n----------- Code from template source -----------------------------\r\n";
                                                actString += fromCode.substring(nfa.getPos(),
                                                        (fromCode.length() < nfa.getEndPos() ? fromCode.length()
                                                                : nfa.getEndPos()));
                                                actString += "\r\n-------------------------------------------------------------------\r\n";
                                            }

                                            if (nfa != null) {
                                                nfa = matcherAst.getMappings().getSrc(nfa);
                                                if (nfa != null && nfa.getLength() > 0) {
                                                    actString += "\r\n----------- Code from template destination--------------------------\r\n";
                                                    actString += toCode.substring(nfa.getPos(),
                                                            (toCode.length() < nfa.getEndPos() ? toCode.length()
                                                                    : nfa.getEndPos()));
                                                    actString += "\r\n-------------------------------------------------------------------\r\n";
                                                }
                                            }
                                        }

                                    }

                                    emuCode += actString + "\n";
                                    if (seekString.length() > 0) {
                                        if (!seekCode.containsKey(seekString))
                                            seekCode.put(seekString, new seekItem(seekString, actString));
                                    }

                                }

                                BufferedWriter writer = null;

                                sizes[i] = seekCode.size();

                                if (verbose.equals("yes")) {
                                    if (seekCode.size() >= minCountOfMarkers) {
                                        writer = null;
                                        writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                        writer.write(emuCode);
                                        writer.close();

                                        writer = null;
                                        writer = new BufferedWriter(new FileWriter(seekFile.getAbsolutePath()));
                                        for (seekItem si : seekCode.values()) {
                                            writer.write(si.seekString + "\r\n");
                                        }
                                        writer.close();
                                        writer = null;
                                    }
                                }

                            }
                            fromSolution = null;
                            toSolution = null;
                        }
                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }

                    if (seekCode.size() >= minCountOfMarkers) {

                        // scan all dataset 1 for test with template item from dataset 2
                        for (int j = 0; j < defectFiles1.size(); j++) {
                            System.out.println(">>>>" + i + "(" + defectFiles2.size() + ")" + " x " + j + "("
                                    + defectFiles1.size() + ")");

                            weightMatrix[i][j] = 0;

                            String defectA = defectFiles1.get(j);
                            TreeContext srcA = null;

                            try {
                                srcA = null;
                                var fromCodeA = Files.readString(Paths.get(defectA));
                                if (fromCodeA.length() <= MAX_FILE_SIZE) {
                                    var fromSolutionA = new Solution(fromCodeA, "A_BAD", "A_BAD", FAIL);
                                    srcA = generator.buildTreeContext(fromSolutionA);
                                    fromSolutionA = null;
                                }

                                if (srcA != null && dstB != null && actB != null) {
                                    List<String> seekCheck = new ArrayList<String>();

                                    if (seekCode.size() >= minCountOfMarkers) {

                                        List<ITree> po = TreeUtils.preOrder(srcA.getRoot());
                                        for (ITree n : po) {
                                            String c = ActionContext.GetNodePath(n, false, srcA);
                                            if (c.length() > 0) {
                                                if (seekCode.containsKey(c)) {
                                                    if (!seekCheck.contains(c))
                                                        seekCheck.add(c);
                                                }

                                            }
                                            // if all strings are found we can stop checking
                                            if (seekCheck.size() == seekCode.size())
                                                break;
                                        }

                                        weightMatrix[i][j] = 100.0 * seekCheck.size() / seekCode.size();
                                    }
                                    seekCheck.clear();
                                    seekCheck = null;
                                }

                            } catch (Exception any) {
                                System.out.println(any.getMessage());
                                any.printStackTrace();
                            }

                        }

                        // write csv result for given (each) defectB
                        {

                            int Size = 0, Cnt = 0;
                            String calcDefect = "";
                            for (int j = 0; j < defectFiles1.size(); j++) {
                                if (weightMatrix[i][j] == 100.0)
                                    Cnt++;
                                if (weightMatrix[i][j] > 0.0)
                                    Size++;
                                // Size += weightMatrix[i][j];
                            }

                            for (String defect : defects2) {
                                if (defectFiles2.get(i).contains("\\" + defect + "\\")) {

                                    calcDefect = defect;
                                    break;
                                }
                            }

                            String matrixFile = pathToMatrix.toString() + "\\";
                            // matrixFile += "(" + calcDefect + ")_" +Cnt + "_" + Size + "_seek_"+ sizes[i]
                            // + ".csv";
                            matrixFile += calcDefect + "_F." + Cnt + "_P." + Size + "_S." + sizes[i] + ".csv";
                            BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));
                            writer.write("\"" + calcDefect + "\"," + Cnt + "," + Size + "\r\n");
                            {

                                writer.write("\"base\"");
                                for (int j = 0; j < defectFiles1.size(); j++) {
                                    for (String defect : defects1) {
                                        if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                                            writer.write(",\"" + defect + "\"");
                                            break;
                                        }
                                    }
                                }
                                writer.write("\r\n\"" + calcDefect + "\"");
                                for (int j = 0; j < defectFiles1.size(); j++) {
                                    writer.write("," + weightMatrix[i][j]);
                                }
                                writer.write("\r\n");
                            }
                            writer.close();
                        }

                    }

                }

                String matrixFile = pathToMatrix.toString() + "\\looklike.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                // first row - list of defects to test
                writer.write("\"template to defect\"");
                for (int j = 0; j < defectFiles1.size(); j++) {

                    for (String defect : defects1) {
                        if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                            writer.write(",\"" + defect + "\"");
                            break;
                        }
                    }
                }
                writer.write("\r\n");

                // other rows one per template defect
                for (int i = 0; i < defectFiles2.size(); i++) {

                    for (String defect : defects2) {
                        if (defectFiles2.get(i).contains("\\" + defect + "\\")) {
                            writer.write("\"" + defect + "\"");
                            break;
                        }
                    }
                    for (int j = 0; j < defectFiles1.size(); j++) {
                        writer.write("," + weightMatrix[i][j]);
                    }
                    writer.write("\r\n");
                }
                writer.close();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void Suggestion(Path pathToFile, Path pathToBugLib, Path pathToListFile, Path pathToSuggestion,
            String verbose, Integer minCountOfMarkers, Integer minSugestionSimilarityLevel, Boolean UseAnyNode) throws IOException {

        System.out.println("Markers: " + minCountOfMarkers);    
        System.out.println("Minimal Suggestion level: " + minSugestionSimilarityLevel);    
        System.out.println("Use any node: " + UseAnyNode);    

        // check directory structure
        File directory = new File(pathToSuggestion.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToSuggestion.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        final String testFileName = pathToFile.toString();
        List<suggestion> sugList = new ArrayList<suggestion>();

        try {

            // bug library ( bad + good files)
            List<String> defects2 = Files.readAllLines(pathToListFile);

            List<String> defectFiles2 = new ArrayList<String>();

            final String badFolderName2 = pathToBugLib.toString() + "\\bad";
            final String goodFolderName2 = pathToBugLib.toString() + "\\good";
            final String astFolderName = pathToSuggestion.toString() + "\\ast";

            List<String> result2 = Files.walk(Paths.get(badFolderName2)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result2) {
                boolean useFile = false;

                for (String defect : defects2) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles2.add(fName);
                }
            }

            if (testFileName.length() > 0 && defectFiles2.size() > 0) {

                final String defectA = testFileName;
                     
                var fromCodeA = Files.readString(Paths.get(defectA));
                String [] lines = fromCodeA.split("\n");
                int[] LinePos = new int[lines.length];
                int curPos=0;
                for(int p=0;p<lines.length;p++){
                    LinePos[p]=curPos;
                    curPos+= lines[p].length()+1;
                    //System.out.println("L:" + p + " pos:" +  LinePos[p] + " length:" + lines[p].length());
                }

                
                if (fromCodeA.length() <= MAX_FILE_SIZE) {
                    var fromSolutionA = new Solution(fromCodeA, "A_BAD", "A_BAD", FAIL);
                    ASTGenerator generator1 = new CachedASTGenerator(new BasicASTNormalizer());  
                    final TreeContext srcA =  generator1.buildTreeContext(fromSolutionA);
                    fromSolutionA = null;
                    generator1 = null;

                // collect common actions for cluster here
                double[] weightMatrix = new double[defectFiles2.size()];
                int[] sizes = new int[defectFiles2.size()];

                // ------------------ threaded zone
                //List<Thread> Threads = new ArrayList<Thread>();
                List<Runnable> Tasks = new ArrayList<Runnable>();

                ExecutorService es =  Executors.newFixedThreadPool(4);
               
                for (int i = 0; i < defectFiles2.size(); i++) {
                    final int idx = i;


                    //Thread myThread = new Thread(new Runnable() {
                    Tasks.add( new Runnable() {
                        public void run() // Этот метод будет выполняться в побочном потоке
                        {

                            System.out.println(idx +" start");
                            final ASTGenerator generator = new CachedASTGenerator(new BasicASTNormalizer());

                            // get template defects from second dataset
                            String defectB = defectFiles2.get(idx);
                            String defectB_Name = "";

                            for (String defect : defects2) {

                                if (defectB.contains("\\" + defect + "\\")) {
                                    defectB_Name = defect;
                                    break;
                                }
                            }

                            TreeContext dstB = null;
                            List<Action> actB = null;

                            Map<String, seekItem> seekCode = new HashMap<String, seekItem>();
                            String emuCode = "";

                            File actionsFile = new File(astFolderName + "//" + defectB_Name + ".ES");
                            File seekFile = new File(astFolderName + "//" + defectB_Name + ".seek");

                            try {

                                var fromCode = Files.readString(Paths.get(defectB));
                                var toCode = Files
                                        .readString(Paths.get(defectB.replace(badFolderName2, goodFolderName2)));
                                if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                                    //System.out.println(idx +" sizes: " + fromCode.length() +"->" + toCode.length() );
                                    var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                                    var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                                    TreeContext srcB = null;

                                    srcB = generator.buildTreeContext(fromSolution);
                                    dstB = generator.buildTreeContext(toSolution);

                                    Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(),
                                            dstB.getRoot());
                                    //System.out.print(".");
                                    try {
                                        matcherAst.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                    }

                                    ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(),
                                            dstB.getRoot(), matcherAst.getMappings());
                                    try {
                                        actionGenerator.generate();
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        e.printStackTrace();
                                    }

                                    actB = actionGenerator.getActions();

                                    if (actB != null && actB.size() > 0) {
                                        seekCode.clear();
                                        emuCode = "";

                                        for (Action action : actB) {

                                            ITree actNode = null;
                                            String actString = action.getName();
                                            String seekString = "";

                                            if (action.getName() == "UPD") {
                                                Update u = (Update) action;
                                                actNode = u.getNode();
                                                actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                                if (UseAnyNode || actNode.hasLabel())
                                                    seekString += ActionContext.GetContextPath(action, false, srcB);

                                                actString += " change to " + u.getValue();
                                            }

                                            if (action.getName() == "MOV" || action.getName() == "INS") {
                                                Addition ad = (Addition) action;
                                                actNode = ad.getParent();
                                                actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                                if (UseAnyNode || actNode.hasLabel())
                                                    seekString += ActionContext.GetContextPath(action, false, srcB);

                                            }

                                            if (action.getName() == "DEL") {
                                                Delete d = (Delete) action;
                                                actNode = d.getNode();
                                                actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                                if (UseAnyNode || actNode.hasLabel())
                                                    seekString += ActionContext.GetContextPath(action, false, srcB);

                                            }

                                            if (verbose.equals("yes")) {
                                                if (seekString.length() > 0) {
                                                    ITree nfa = ActionContext.GetContextRoot(action);

                                                    if (nfa != null && nfa.getLength() > 0) {
                                                        actString += "\r\n----------- Code from template source -----------------------------\r\n";
                                                        actString += fromCode.substring(nfa.getPos(),
                                                                (fromCode.length() < nfa.getEndPos() ? fromCode.length()
                                                                        : nfa.getEndPos()));
                                                        actString += "\r\n-------------------------------------------------------------------\r\n";
                                                    }

                                                    if (nfa != null) {
                                                        nfa = matcherAst.getMappings().getSrc(nfa);
                                                        if (nfa != null && nfa.getLength() > 0) {
                                                            actString += "\r\n----------- Code from template destination--------------------------\r\n";
                                                            actString += toCode.substring(nfa.getPos(),
                                                                    (toCode.length() < nfa.getEndPos() ? toCode.length()
                                                                            : nfa.getEndPos()));
                                                            actString += "\r\n-------------------------------------------------------------------\r\n";
                                                        }
                                                    }
                                                }

                                            }

                                            emuCode += actString + "\n";
                                            if (seekString.length() > 0) {
                                                if (!seekCode.containsKey(seekString))
                                                    seekCode.put(seekString, new seekItem(seekString, actString));
                                            }

                                        }

                                        BufferedWriter writer = null;

                                        sizes[idx] = seekCode.size();

                                        if (verbose.equals("yes")) {
                                            if (seekCode.size() >= minCountOfMarkers) {
                                                writer = null;
                                                writer = new BufferedWriter(
                                                        new FileWriter(actionsFile.getAbsolutePath()));
                                                writer.write(emuCode);
                                                writer.close();

                                                writer = null;
                                                writer = new BufferedWriter(new FileWriter(seekFile.getAbsolutePath()));
                                                for (seekItem si : seekCode.values()) {
                                                    writer.write(si.seekString + "\r\n");
                                                }
                                                writer.close();
                                                writer = null;
                                            }
                                        }

                                    }
                                    fromSolution = null;
                                    toSolution = null;
                                }
                            } catch (Exception any) {
                                System.out.println(any.getMessage());
                                any.printStackTrace();
                            }

                            if (seekCode.size() >= minCountOfMarkers) {
                                weightMatrix[idx] = 0;
                                try {
                                    if (srcA != null && dstB != null && actB != null) {
                                        List<String> seekCheck = new ArrayList<String>();

                                        //if (seekCode.size() >= minCountOfMarkers) {

                                            suggestion sug = new suggestion(defectB_Name);

                                            List<ITree> po = TreeUtils.preOrder(srcA.getRoot());
                                            for (ITree n : po) {
                                                String c = ActionContext.GetNodePath(n, false, srcA);
                                                if (c.length() > 0) {
                                                    if (seekCode.containsKey(c)) {
                                                        if (!seekCheck.contains(c)) {
                                                            seekCheck.add(c);
                                                            suggestionItem sugItem = new suggestionItem(n.getId(),
                                                                    n.getPos(), n.getLength(),
                                                                    seekCode.get(c).actionString,
                                                                    seekCode.get(c).seekString);
                                                            sug.suggestions.add(sugItem);
                                                        }

                                                    }

                                                }
                                                // if all strings are found we can stop checking
                                                if (seekCheck.size() == seekCode.size())
                                                    break;
                                            }

                                            weightMatrix[idx] = 100.0 * seekCheck.size() / seekCode.size();

                                            if (weightMatrix[idx] >= minSugestionSimilarityLevel || weightMatrix[idx] == 100.0 ) {
                                                sugList.add(sug);
                                                sug = null;
                                            }

                                            
                                            System.out.println("thread["+idx + "] defect:" + defectB_Name+" similarity: " + weightMatrix[idx]);
                                        //}
                                        seekCheck.clear();
                                        seekCheck = null;
                                    }


                                } catch (Exception any) {
                                    System.out.println(any.getMessage());
                                    any.printStackTrace();
                                }

                            }
                            System.out.println(idx +" done");

                        }
                        
                    });


                    //Threads.add(myThread);
                    //myThread.start();

                }

                CompletableFuture<?>[] futures = Tasks.stream()
                               .map(task -> CompletableFuture.runAsync(task, es))
                               .toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(futures).join();    
                es.shutdown();


               

                //System.out.println("");
                System.out.println("Save results");
                String matrixFile = pathToSuggestion.toString() + "\\";
                matrixFile +=  "similarity.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                for (int i = 0; i < defectFiles2.size(); i++) {

                    // get template defects from second dataset
                    String defectB = defectFiles2.get(i);
                    String calcDefect = "";
    
                    for (String defect : defects2) {
    
                        if (defectB.contains("\\" + defect + "\\")) {
                            calcDefect = defect;
                            break;
                        }
                    }
                    writer.write("\"" + calcDefect + "\"");
                    writer.write("," + weightMatrix[i]);
                    writer.write("\r\n");
                }
                writer.close();


                writer = new BufferedWriter(new FileWriter( pathToSuggestion.toString() + "\\suggestions.json"));
                writer.write("{\"suggestions\":[\r\n");
                boolean firstSug = true;
                for (suggestion sug : sugList) {
                    if(!firstSug)
                        writer.write(",");
                    firstSug=false;
                    writer.write("{\"suggestFrom\":\"" + sug.BugLibraryItem + "\",\"items\":\r\n[");
                    boolean firstItem = true;
                    for(suggestionItem sugItem : sug.suggestions){
                        if(!firstItem)
                            writer.write(",");
                        firstItem=false;
                        writer.write("\r\n\t{\r\n");
                        if(sugItem.startPosition >0){
                            int p2=0;
                            int col=0;
                            for (int p=0;p<LinePos.length;p++){
                                if( LinePos[p] >= sugItem.startPosition ){
                                    p2=p-1;
                                    col =sugItem.startPosition - LinePos[p2] ;
                                    if(p2>0){
                                        col+=2;
                                        writer.write("\"line\":" + p +",");            
                                        writer.write("\"column\":" + col +",");            // + 1 +\n ?
                                    }
                                    if(p2==0){
                                        col+=1;
                                        writer.write("\"line\":" + p +",");            
                                        writer.write("\"column\":" + col +",");            // + 1 
                                    }
                                    break;
                                }
                            }
                            //System.out.println("pos: " + sugItem.startPosition + " l:" + (p2+1) + " c:"+ col);
                        }else{
                            writer.write("\"line\": 0,");            
                            writer.write("\"column\":0,");            // + 1 ?
                        }


                        writer.write("\"position\":" +sugItem.startPosition +",");
                        writer.write("\"length\":" +sugItem.endPosition +",");
                        writer.write("\"modification\":\"" +sugItem.SuggestionContent.replace("\\", "\\\\").replace("\"","\\\"") +"\",");
                        writer.write("\"reason\":\"" + sugItem.reson.replace("\\", "\\\\") .replace("\"","\\\"") +"\"");

                        writer.write("}\r\n");
                    }
                    
                    writer.write("]\r\n");
                    writer.write("}\r\n");
                }
                writer.write("]}\r\n");
                writer.close();


               

            }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        
           
    }



    public static void LaseLookLikeCluster(Path pathToDataset, Path pathToClusterFile,  Path pathToMatrix, String version, String verbose, Integer minCountOfMarkers, Boolean UseAnyNode) throws IOException {

        System.out.println("Markers: " + minCountOfMarkers);    
        System.out.println("Use any node: " + UseAnyNode);    

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToMatrix.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum = 0;

        for (String cluster : clusters) {
            String[] defects1 = cluster.split(" ");
            clusterNum++;

            System.out.println("Cluster #"+ clusterNum );
            System.out.println( cluster );
            // first dataset - test (bad files only)
            
            List<String> defectFiles1 = new ArrayList<String>();

            String badFolderName1 = pathToDataset.toString() + "\\bad";

            try {

                List<String> result1 = Files.walk(Paths.get(badFolderName1)).filter(Files::isRegularFile)
                        .map(x -> x.toString()).collect(Collectors.toList());

                // collect all files for defect to build matrix
                for (String fName : result1) {
                    boolean useFile = false;

                    for (String defect : defects1) {

                        if (fName.contains("\\" + defect + "\\")) {
                            var Code = Files.readString(Paths.get(fName));
                            if (Code.length() <= MAX_FILE_SIZE)
                                useFile = true;
                            break;
                        }
                    }
                    if (useFile) {
                        defectFiles1.add(fName);
                    }
                }

                

                String badFolderName2 = pathToDataset.toString() + "\\bad";
                String goodFolderName2 = pathToDataset.toString() + "\\good";
                String astFolderName = pathToMatrix.toString() + "\\ast";

                // defect files is a collection of bad files
                if (defectFiles1.size() > 1 ) {

                    // collect common actions for cluster here
                    double[][] weightMatrix = new double[defectFiles1.size()][defectFiles1.size()];
                    int[] sizes = new int[defectFiles1.size()];

                    ASTGenerator generator = null;

                    if (version.toLowerCase().equals("abstract")) {
                        generator = new CachedASTGenerator(new NamesASTNormalizer());
                    } else {
                        generator = new CachedASTGenerator(new BasicASTNormalizer());
                    }

                    for (int i = 0; i < defectFiles1.size(); i++) {

                        // get template defects from second dataset
                        String defectB = defectFiles1.get(i);
                        String defectB_Name = "";

                        for (String defect : defects1) {

                            if (defectB.contains("\\" + defect + "\\")) {
                                defectB_Name = defect;
                                break;
                            }
                        }

                        TreeContext dstB = null;
                        List<Action> actB = null;
                   
                        Map<String, seekItem> seekCode = new HashMap<String, seekItem>();
                        String emuCode = "";

                        File actionsFile = new File(astFolderName + "//" + defectB_Name + ".ES");
                        File seekFile = new File(astFolderName + "//" + defectB_Name + ".seek");

                        try {
                            var fromCode = Files.readString(Paths.get(defectB));
                            var toCode = Files.readString(Paths.get(defectB.replace(badFolderName2, goodFolderName2)));
                            if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                                var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                                var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                                TreeContext srcB = null;

                                srcB = generator.buildTreeContext(fromSolution);
                                dstB = generator.buildTreeContext(toSolution);

                                Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                                // System.out.println("Compare trees");
                                try {
                                    matcherAst.match();
                                } catch (NullPointerException e) {
                                    System.out.println(e.getMessage());
                                }

                                ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(), dstB.getRoot(),
                                        matcherAst.getMappings());
                                try {
                                    actionGenerator.generate();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                }

                                actB = actionGenerator.getActions();

                                if (actB != null && actB.size() > 0) {
                                    seekCode.clear();
                                    emuCode = "";

                                    for (Action action : actB) {

                                        ITree actNode = null;
                                        String actString = action.getName();
                                        String seekString = "";

                                        if (action.getName() == "UPD") {
                                            Update u = (Update) action;
                                            actNode = u.getNode();
                                            actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                            if (UseAnyNode || actNode.hasLabel())
                                                seekString += ActionContext.GetContextPath(action, false, srcB);

                                            actString += " change to " + u.getValue();
                                        }

                                        if (action.getName() == "MOV" || action.getName() == "INS") {
                                            Addition ad = (Addition) action;
                                            actNode = ad.getParent();
                                            actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                            if (UseAnyNode ||actNode.hasLabel())
                                                seekString += ActionContext.GetContextPath(action, false, srcB);

                                        }

                                        if (action.getName() == "DEL") {
                                            Delete d = (Delete) action;
                                            actNode = d.getNode();
                                            actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                            if (UseAnyNode || actNode.hasLabel())
                                                seekString += ActionContext.GetContextPath(action, false, srcB);

                                        }

                                        if (verbose.equals("yes")) {
                                            if (seekString.length() > 0) {
                                                ITree nfa = ActionContext.GetContextRoot(action);

                                                if (nfa != null && nfa.getLength() > 0) {
                                                    actString += "\r\n----------- Code from template source -----------------------------\r\n";
                                                    actString += fromCode.substring(nfa.getPos(),
                                                            (fromCode.length() < nfa.getEndPos() ? fromCode.length()
                                                                    : nfa.getEndPos()));
                                                    actString += "\r\n-------------------------------------------------------------------\r\n";
                                                }

                                                if (nfa != null) {
                                                    nfa = matcherAst.getMappings().getSrc(nfa);
                                                    if (nfa != null && nfa.getLength() > 0) {
                                                        actString += "\r\n----------- Code from template destination--------------------------\r\n";
                                                        actString += toCode.substring(nfa.getPos(),
                                                                (toCode.length() < nfa.getEndPos() ? toCode.length()
                                                                        : nfa.getEndPos()));
                                                        actString += "\r\n-------------------------------------------------------------------\r\n";
                                                    }
                                                }
                                            }

                                        }

                                        emuCode += actString + "\n";
                                        if (seekString.length() > 0) {
                                            if (!seekCode.containsKey(seekString))
                                                seekCode.put(seekString, new seekItem(seekString, actString));
                                        }

                                    }

                                    BufferedWriter writer = null;

                                    sizes[i] = seekCode.size();

                                    if (verbose.equals("yes")) {
                                        if (seekCode.size() >= minCountOfMarkers) {
                                            writer = null;
                                            writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                            writer.write(emuCode);
                                            writer.close();

                                            writer = null;
                                            writer = new BufferedWriter(new FileWriter(seekFile.getAbsolutePath()));
                                            for (seekItem si : seekCode.values()) {
                                                writer.write(si.seekString + "\r\n");
                                            }
                                            writer.close();
                                            writer = null;
                                        }
                                    }

                                }
                                fromSolution = null;
                                toSolution = null;
                            }
                        } catch (Exception any) {
                            System.out.println(any.getMessage());
                            any.printStackTrace();
                        }

                        if (seekCode.size() >= minCountOfMarkers) {

                            // scan all dataset 1 for test with template item from dataset 2
                            for (int j = 0; j < defectFiles1.size(); j++) {
                                System.out.println(">>>>" + i + "(" + defectFiles1.size() + ")" + " x " + j + "("
                                        + defectFiles1.size() + ")");

                                weightMatrix[i][j] = 0;

                                String defectA = defectFiles1.get(j);
                                TreeContext srcA = null;

                                try {
                                    srcA = null;
                                    var fromCodeA = Files.readString(Paths.get(defectA));
                                    if (fromCodeA.length() <= MAX_FILE_SIZE) {
                                        var fromSolutionA = new Solution(fromCodeA, "A_BAD", "A_BAD", FAIL);
                                        srcA = generator.buildTreeContext(fromSolutionA);
                                        fromSolutionA = null;
                                    }

                                    if (srcA != null ) {
                                        List<String> seekCheck = new ArrayList<String>();

                                        if (seekCode.size() >= minCountOfMarkers) {

                                            List<ITree> po = TreeUtils.preOrder(srcA.getRoot());
                                            for (ITree n : po) {
                                                String c = ActionContext.GetNodePath(n, false, srcA);
                                                if (c.length() > 0) {
                                                    if (seekCode.containsKey(c)) {
                                                        if (!seekCheck.contains(c))
                                                            seekCheck.add(c);
                                                    }

                                                }
                                                // if all strings are found we can stop checking
                                                if (seekCheck.size() == seekCode.size())
                                                    break;
                                            }

                                            weightMatrix[i][j] = 100.0 * seekCheck.size() / seekCode.size();
                                        }
                                        seekCheck.clear();
                                        seekCheck = null;
                                    }

                                } catch (Exception any) {
                                    System.out.println(any.getMessage());
                                    any.printStackTrace();
                                }

                            }

                            // write csv result for given (each) defectB
                            {

                                int Size = 0, Cnt = 0;
                                String calcDefect = "";
                                for (int j = 0; j < defectFiles1.size(); j++) {
                                    if (weightMatrix[i][j] == 100.0)
                                        Cnt++;
                                    if (weightMatrix[i][j] > 0.0)
                                        Size++;
                                    // Size += weightMatrix[i][j];
                                }

                                for (String defect : defects1) {
                                    if (defectFiles1.get(i).contains("\\" + defect + "\\")) {

                                        calcDefect = defect;
                                        break;
                                    }
                                }

                                String matrixFile = pathToMatrix.toString() + "\\";
                                // matrixFile += "(" + calcDefect + ")_" +Cnt + "_" + Size + "_seek_"+ sizes[i]
                                // + ".csv";
                                matrixFile += "C." +clusterNum+ "_"+ calcDefect + "_F." + Cnt + "_P." + Size + "_S." + sizes[i] +  ".csv";
                                BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));
                                writer.write("\"" + calcDefect + "\"," + Cnt + "," + Size + "\r\n");
                                {

                                    writer.write("\"base\"");
                                    for (int j = 0; j < defectFiles1.size(); j++) {
                                        for (String defect : defects1) {
                                            if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                                                writer.write(",\"" + defect + "\"");
                                                break;
                                            }
                                        }
                                    }
                                    writer.write("\r\n\"" + calcDefect + "\"");
                                    for (int j = 0; j < defectFiles1.size(); j++) {
                                        writer.write("," + weightMatrix[i][j]);
                                    }
                                    writer.write("\r\n");
                                }
                                writer.close();
                            }

                        }else{
                            System.out.println(">>>>" + i + "(" + defectFiles1.size() + ") skip - not enough markers");
                        }

                    }

                    String matrixFile = pathToMatrix.toString() + "\\C." +clusterNum+ "_looklike.csv";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                    // first row - list of defects to test
                    writer.write("\"template to defect\"");
                    for (int j = 0; j < defectFiles1.size(); j++) {

                        for (String defect : defects1) {
                            if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                                writer.write(",\"" + defect + "\"");
                                break;
                            }
                        }
                    }
                    writer.write("\r\n");

                    // other rows one per template defect
                    for (int i = 0; i < defectFiles1.size(); i++) {

                        for (String defect : defects1) {
                            if (defectFiles1.get(i).contains("\\" + defect + "\\")) {
                                writer.write("\"" + defect + "\"");
                                break;
                            }
                        }
                        for (int j = 0; j < defectFiles1.size(); j++) {
                            writer.write("," + weightMatrix[i][j]);
                        }
                        writer.write("\r\n");
                    }
                    writer.close();

                }else{
                    System.out.println("Cluster has only one defect. Skipped");
                }
            

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }



}
