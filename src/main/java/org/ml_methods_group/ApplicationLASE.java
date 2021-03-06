
package org.ml_methods_group;

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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ApplicationLASE extends ApplicationMethods {

    protected static List<String> BuildCommonActions(List<String> A, List<String> B) {
        int maxCnt = 0;
        int posMaxA = -1;
        for (int i = 0; i < A.size(); i++) {
            for (int j = 0; j < B.size(); j++) {
                int k = i;
                int l = j;
                int cnt = 0;
                while (k < A.size() && l < B.size() && A.get(k).equals(B.get(l))) {
                    cnt++;
                    // System.out.println("A[" +k +"] = B["+ l +"], cnt=" + cnt);
                    k++;
                    l++;
                }

                if (cnt > maxCnt) {
                    maxCnt = cnt;
                    posMaxA = i;
                    System.out.println("New max at " + i + " = " + maxCnt);
                }

            }
        }
        List<String> result = new ArrayList<>();
        if (maxCnt > 0) {
            for (int i = 0; i < maxCnt; i++) {
                result.add(A.get(posMaxA + i));
            }
        }
        System.out.println("result CA: " + result.size());

        return result;
    }

    public static void prepareLASEDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList,
            String version) throws IOException {

        int processed = 0;
        int skipped = 0;

        String badFolderName = pathToDataset.toString() + "\\bad";
        String goodFolderName = pathToDataset.toString() + "\\good";
        List<String> defects = Files.readAllLines(pathToBugList);

        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        try {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            var baseTime = System.currentTimeMillis();

            for (String fName : result) {
                Boolean useFile = false;
                for (String defect : defects) {
                    if (fName.contains(defect)) {
                        useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    try {

                        baseTime = System.currentTimeMillis();
                        processed++;
                        System.out.println("******************* found: " + processed + ", skipped: " + skipped);

                        Path methodBeforePath = Paths.get(fName);
                        Path methodAfterPath = Paths.get(fName.replace(badFolderName, goodFolderName));
                        String[] paths = splitPath(fName.replace(badFolderName, ""));

                        String defectId = paths[0] + "_" + version + "_" + paths[paths.length - 1];
                        String seekId = paths[0] + ".seek.txt";

                        System.out.println(getDiff(baseTime) + ": Defect id: " + defectId);

                        File fromFile = methodBeforePath.toFile();
                        File toFile = methodAfterPath.toFile();

                        File actionsFile = new File(pathToSaveRepresentations.toString() + "\\" + defectId);
                        File seekFile = new File(pathToSaveRepresentations.toString() + "\\" + seekId);

                        String rightSolutionId = defectId + "_" + OK.ordinal();
                        String wrongSolutionId = defectId + "_" + FAIL.ordinal();

                        if (fromFile.length() > 0 && toFile.length() > 0) {
                            System.out.println("Sizes: " + fromFile.length() + " ->" + toFile.length());
                           
                            System.out.println(getDiff(baseTime) + ": Checking size");
                            if (fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE ) {
                                String emuCode = "";
                                String seekCode = "";

                                if (actionsFile.exists()) {
                                    System.out.println(getDiff(baseTime) + ": repared file exists");
                                } else {

                                    // write empty file for skip crash at next pass
                                    BufferedWriter writer = new BufferedWriter(
                                            new FileWriter(actionsFile.getAbsolutePath()));
                                    writer.write("{}");
                                    writer.close();

                                    var fromCode = Files.readString(methodBeforePath);
                                    var toCode = Files.readString(methodAfterPath);

                                    System.out.println(getDiff(baseTime) + ": Files loaded");

                                    var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                    var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                    System.out.println(getDiff(baseTime) + ": Building source actions");

                                    TreeContext src;
                                    TreeContext dst;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(new NamesASTNormalizer());
                                    } else {
                                        generator = new CachedASTGenerator(new BasicASTNormalizer());
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    dst = generator.buildTreeContext(toSolution);

                                    Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
                                    try {
                                        matcher.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());

                                    }
                                    ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(), dst.getRoot(),
                                            matcher.getMappings());
                                    try {
                                        actionGenerator.generate();
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                        e.printStackTrace();
                                    }

                                    final List<Action> actions = actionGenerator.getActions();
                                    fromSolution = null;
                                    toSolution = null;

                                    if (actions != null && actions.size() > 0) {
                                        System.out.println(getDiff(baseTime) + ": Prepare es");

                                        emuCode = "";
                                        seekCode = "";

                                        // store Actions
                                        // int idx=0;
                                        for (Action action : actions) {
                                            
                                            ITree actNode= null;
                                            String actString = action.getName();
                                            String seekString = "";
                                           
                                           
                                            

                                            if(action.getName()=="UPD"){
                                                Update u = (Update) action;
                                                actNode = u.getNode();
                                                actString += " " + ActionContext.GetContextPath(action, false, src);
                                                seekString+=ActionContext.GetContextPath(action, false, src);
                                                //actString += (actNode.hasLabel() ? " " + actNode.getLabel()
                                                //                .replace("\r", " ").replace("\n", " ") : "");
                                                actString += " change to " + u.getValue();
                                            }

                                            if(action.getName()=="MOV" || action.getName()=="INS" ){
                                                Addition ad = (Addition) action;
                                                actNode = ad.getParent();
                                                actString += " " + ActionContext.GetContextPath(action, false, src);
                                                if (actNode.hasLabel())
                                                    seekString+=ActionContext.GetContextPath(action, false, src);
                                                //actString += (actNode.hasLabel() ? " " + actNode.getLabel()
                                                //                .replace("\r", " ").replace("\n", " ") : "");
                                                
                                            }

                                            if(action.getName()=="DEL"){
                                                Delete d = (Delete) action;
                                                actNode = d.getNode();
                                                actString += " " + ActionContext.GetContextPath(action, false, src);
                                                if (actNode.hasLabel())
                                                    seekString+=ActionContext.GetContextPath(action, false, src);
                                                //actString += (actNode.hasLabel() ? " " + actNode.getLabel()
                                                //                .replace("\r", " ").replace("\n", " ") : "");
                                            }

                                            ITree nfa = ActionContext.GetContextRoot(action);
                                            if(nfa != null && nfa.getLength() >0){
                                                actString +="\r\n----------- Code from template source -----------------------------\r\n";
                                                actString +=fromCode.substring( nfa.getPos(), (fromCode.length()<nfa.getEndPos()?fromCode.length():nfa.getEndPos()) );    
                                                actString +="\r\n-------------------------------------------------------------------\r\n";
                                            }

                                            if(nfa != null){
                                                nfa = matcher.getMappings().getSrc(nfa);
                                                if(nfa != null && nfa.getLength() >0){
                                                    actString +="\r\n----------- Code from template destination--------------------------\r\n";
                                                    actString +=toCode.substring( nfa.getPos(), (toCode.length()<nfa.getEndPos()?toCode.length():nfa.getEndPos()) );    
                                                    actString +="\r\n-------------------------------------------------------------------\r\n";
                                                }
                                            }



                                            // idx++;
                                            //TreeIoUtils.toXml(src).writeTo(
                                            //        pathToSaveRepresentations.toString() + "\\ast\\" + defectId);

                                            emuCode += actString + "\n";
                                            if(seekString.length()>0 )
                                                seekCode += seekString + "\n";

                                        }

                                        writer = null;
                                        writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                        writer.write(emuCode);
                                        writer.close();

                                        writer = null;
                                        writer = new BufferedWriter(new FileWriter(seekFile.getAbsolutePath()));
                                        writer.write(seekCode);
                                        writer.close();

                                    } else {
                                        writer = null;
                                        System.out.println(getDiff(baseTime) + ": No actions detected");
                                    }
                                }
                                System.out.println(getDiff(baseTime) + ": Done");
                            } else {
                                skipped++;
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " + defectId
                                        + " Very large file size."); 
                            }

                        }

                        toFile = null;
                        fromFile = null;

                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": All files are prepared");

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    public static void MakeMaxTree(Path pathToDataset, Path pathToSaveRepresentations, String DefectA, String DefectB,
            String version) throws IOException {

        if (DefectA.equals(DefectB)) {
            System.out.println("Defect ID must be different for source (DefectA) and template (DefectB) ");
            return;
        }

        String badFolderName = pathToDataset.toString() + "\\bad";
        String goodFolderName = pathToDataset.toString() + "\\good";

        List<String> defects = new ArrayList<String>();
        defects.add(DefectA);
        defects.add(DefectB);

        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        try {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            var baseTime = System.currentTimeMillis();
            boolean isB = false;
            TreeContext srcA = null;
            // TreeContext srcB= null;
            // TreeContext dstA = null;
            TreeContext dstB = null;
            // Matcher matcherA = null;
            // Matcher matcherB = null ;
            // List<Action> actA = null ;
            List<Action> actB = null;

            for (String fName : result) {
                Boolean useFile = false;
                for (String defect : defects) {
                    if (fName.contains(defect)) {
                        useFile = true;
                        if (defect == DefectA)
                            isB = false;
                        else
                            isB = true;
                        break;
                    }
                }
                if (useFile) {
                    try {

                        baseTime = System.currentTimeMillis();

                        Path methodBeforePath = Paths.get(fName);
                        Path methodAfterPath = Paths.get(fName.replace(badFolderName, goodFolderName));
                        String[] paths = splitPath(fName.replace(badFolderName, ""));

                        String defectId = paths[0] + "_" + version + "_" + paths[paths.length - 1];

                        System.out.println(getDiff(baseTime) + ": Defect id: " + defectId);

                        File fromFile = methodBeforePath.toFile();
                        File toFile = methodAfterPath.toFile();

                        File actionsFile = new File(pathToSaveRepresentations.toString() + "\\" + defectId);

                        String rightSolutionId = defectId + "_" + OK.ordinal();
                        String wrongSolutionId = defectId + "_" + FAIL.ordinal();

                        if (fromFile.length() > 0 && toFile.length() > 0) {
                            System.out.println("Sizes: " + fromFile.length() + " ->" + toFile.length());
                            // double rate = ((double) fromFile.length()) / ((double) toFile.length());
                            System.out.println(getDiff(baseTime) + ": Checking size");
                            if (fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE) {
                                {
                                    var fromCode = Files.readString(methodBeforePath);
                                    var toCode = Files.readString(methodAfterPath);

                                    System.out.println(getDiff(baseTime) + ": Files loaded");

                                    var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                    var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                    TreeContext src = null;
                                    TreeContext dst = null;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(new NamesASTNormalizer());
                                    } else {
                                        generator = new CachedASTGenerator(new BasicASTNormalizer());
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    System.out.println("SRC tree size=" + src.getRoot().getSize());

                                    TreeIoUtils.toXml(src)
                                            .writeTo(pathToSaveRepresentations.toString() + "\\ast\\src_" + defectId);

                                    if (isB) {

                                        dst = generator.buildTreeContext(toSolution);
                                        System.out.println("DST tree size=" + dst.getRoot().getSize());
                                        TreeIoUtils.toXml(dst).writeTo(
                                                pathToSaveRepresentations.toString() + "\\ast\\dst_" + defectId);

                                        Matcher matcherAst = Matchers.getInstance().getMatcher(src.getRoot(),
                                                dst.getRoot());
                                        System.out.println("Compare trees");
                                        try {
                                            matcherAst.match();
                                        } catch (NullPointerException e) {
                                            System.out.println(e.getMessage());
                                        }

                                        System.out.println("Build AST");

                                        ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(),
                                                dst.getRoot(), matcherAst.getMappings());
                                        try {
                                            actionGenerator.generate();
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                            e.printStackTrace();
                                        }

                                        final List<Action> actions = actionGenerator.getActions();

                                        // srcB = src;
                                        dstB = dst;
                                        // matcherB =matcherAst;
                                        actB = actions;
                                    } else {
                                        srcA = src;
                                        // dstA = dst;
                                        // matcherA =matcherAst;
                                        // actA = actions;
                                        // isB = true;

                                    }

                                    fromSolution = null;
                                    toSolution = null;

                                }
                                System.out.println(getDiff(baseTime) + ": defect processed");
                            } else {
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " + defectId
                                        + " Very large file  size.");
                            }

                        }

                        toFile = null;
                        fromFile = null;

                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": Try to build maxtree");

            if (srcA != null && dstB != null && actB != null) {

                if (actB.size() > 0) {
                    testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(), new MappingStore());

                    try {
                        matcher.match();
                    } catch (NullPointerException e) {
                        System.out.println(e.getMessage());

                    }

                    // single node fro max length tree
                    ITree minSrc = matcher.GetLongestSrcSubtree(actB);

                    TreeContext mSrc = new TreeContext();
                    mSrc.importTypeLabels(dstB);
                    mSrc.setRoot(minSrc);
                    mSrc.getRoot().refresh();

                    try {
                        TreeIoUtils.toXml(mSrc).writeTo(pathToSaveRepresentations.toString() + "\\ast\\maxTree_"
                                + DefectA + "_to_" + DefectB + ".xml");
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                } else {
                    System.out.println("No edit actions for defect2. Tree will be empty");
                }
            }
            System.out.println(getDiff(baseTime) + ": Done");

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    public static void buildLASEbyCluster(Path pathToLaseDataset, Path pathToClusterFile, Path pathToCommonActions,
            String version) throws IOException {

        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum = 0;

        // check directory structure
        File directory = new File(pathToCommonActions.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {

            for (String cluster : clusters) {
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum + ")");

                if (defects.length > 1) {

                    List<String> result = Files.walk(pathToLaseDataset, 1).filter(Files::isRegularFile)
                            .map(x -> x.toString()).collect(Collectors.toList());

                    List<String> defectFiles = new ArrayList<>();

                    // collect all files for defect to build edit sequence
                    for (String fName : result) {
                        boolean useFile = false;

                        for (String defect : defects) {

                            if (fName.contains(defect + "_" + version)) {
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile) {
                            String emuCode = Files.readString(Paths.get(fName));
                            if (!emuCode.equals("{}")) {
                                defectFiles.add(fName);

                            }

                        }
                    }

                    boolean firstFile = true;

                    // collect common actions for cluster here
                    List<String> commonActions = new ArrayList<>();

                    if (defectFiles.size() > 0) {

                        for (String fName : defectFiles) {
                            System.out.println("processing file:" + fName);
                            List<String> actions = Files.readAllLines(Paths.get(fName));
                            if (actions.size() > 0) {

                                //String[] paths = splitPath(fName);

                                // build commoin Edit
                                if (!firstFile) {
                                    System.out.println("Sizes:" + commonActions.size() + " " + actions.size());
                                    commonActions = BuildCommonActions(commonActions, actions);

                                    // if no commonactions stop processing this cluster
                                    if (commonActions.size() == 0) {
                                        System.out.println(
                                                "Stop analizing cluster:" + cluster + ". No common actions detected. ");
                                        break;
                                    }

                                } else {
                                    commonActions = actions;
                                    firstFile = false;
                                }

                            } else {
                                System.out.println("skip file without edit actions: " + fName);
                            }

                        }
                        String CommonActionsName = pathToCommonActions.toAbsolutePath() + "/" + commonActions.size()
                                + "_" + clusterNum + ".txt";

                        BufferedWriter writer = new BufferedWriter(new FileWriter(CommonActionsName));
                        writer.write(cluster + "\r\n");
                        writer.write("_____________________________________________________\r\n");

                        for (String action : commonActions) {
                            writer.write(action + "\r\n");
                        }
                        writer.close();
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void scanCluster(Path pathToDataset, Path pathToClusterFile, Path pathToMatrix, String version)
            throws IOException {
        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum = 0;

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        String badFolderName = pathToDataset.toString() + "\\bad";
        String goodFolderName = pathToDataset.toString() + "\\good";

        try {

            for (String cluster : clusters) {
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum + ")");

                if (defects.length > 1) {

                    List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                            .map(x -> x.toString()).collect(Collectors.toList());

                    List<String> defectFiles = new ArrayList<>();

                    // collect all files for defect to build matrix
                    for (String fName : result) {
                        boolean useFile = false;

                        for (String defect : defects) {

                            if (fName.contains("\\" + defect + "\\")) {
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile) {
                            defectFiles.add(fName);
                        }
                    }

                    // defect files is a collection of bad files
                    if (defectFiles.size() > 0) {

                        // collect common actions for cluster here
                        int[][] weightMatrix = new int[defectFiles.size()][defectFiles.size()];

                        ASTGenerator generator = null;

                        if (version.toLowerCase().equals("abstract")) {
                            generator = new CachedASTGenerator(new NamesASTNormalizer());
                        } else {
                            generator = new CachedASTGenerator(new BasicASTNormalizer());
                        }

                        for (int i = 0; i < defectFiles.size(); i++) {

                            String defectB = defectFiles.get(i);
                            TreeContext dstB = null;
                            List<Action> actB = null;
                            try {
                                var fromCode = Files.readString(Paths.get(defectB));
                                var toCode = Files
                                        .readString(Paths.get(defectB.replace(badFolderName, goodFolderName)));
                                if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                                    var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                                    var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                                    TreeContext srcB = null;

                                    srcB = generator.buildTreeContext(fromSolution);
                                    dstB = generator.buildTreeContext(toSolution);

                                    Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(),
                                            dstB.getRoot());
                                    System.out.println("Compare trees");
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

                                    fromSolution = null;
                                    toSolution = null;
                                }
                            } catch (Exception any) {
                                System.out.println(any.getMessage());
                                any.printStackTrace();
                            }

                            for (int j = 0; j < defectFiles.size(); j++) {
                                weightMatrix[i][j] = 0;
                                if (i != j) {
                                    String defectA = defectFiles.get(j);
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

                                            if (actB.size() > 0) {
                                                testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),
                                                        new MappingStore());

                                                try {
                                                    matcher.match();
                                                } catch (NullPointerException e) {
                                                    System.out.println(e.getMessage());
                                                }

                                                // ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                                                // weightMatrix[i][j] =minSrc.getSize();

                                                List<ITree> forest = matcher.GetLongestForest(actB);
                                                for (ITree minSrc : forest) {
                                                    weightMatrix[i][j] += minSrc.getSize();
                                                }

                                            }
                                        }

                                    } catch (Exception any) {
                                        System.out.println(any.getMessage());
                                        any.printStackTrace();
                                    }

                                }
                            }
                        }

                        // calculate tipical defect for cluster
                        int[] baseSum = new int[defectFiles.size()];
                        int[] baseCnt = new int[defectFiles.size()];
                        for (int i = 0; i < defectFiles.size(); i++) {
                            baseSum[i] = 0;
                            baseCnt[i] = 0;

                            for (int j = 0; j < defectFiles.size(); j++) {
                                if (weightMatrix[i][j] > 1)
                                    baseCnt[i]++;
                                baseSum[i] += weightMatrix[i][j];
                            }
                        }

                        String matrixFile = pathToMatrix.toString() + "\\matrix_" + clusterNum + ".csv";
                        BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                        writer.write("\"base\"");
                        for (int j = 0; j < defectFiles.size(); j++) {

                            for (String defect : defects) {
                                if (defectFiles.get(j).contains("\\" + defect + "\\")) {
                                    writer.write(",\"" + defect + "\"");
                                    break;
                                }
                            }
                        }
                        writer.write("\r\n");

                        int maxIdx = 0;
                        for (int i = 0; i < defectFiles.size(); i++) {
                            if (baseCnt[i] > baseCnt[maxIdx])
                                maxIdx = i;
                            else if (baseCnt[i] == baseCnt[maxIdx]) {
                                if (baseSum[i] > baseSum[maxIdx])
                                    maxIdx = i;
                            }

                        }

                        for (int i = 0; i < defectFiles.size(); i++) {

                            for (String defect : defects) {
                                if (defectFiles.get(i).contains("\\" + defect + "\\")) {
                                    if (i == maxIdx)
                                        writer.write("\"* " + defect + "\"");
                                    else
                                        writer.write("\"" + defect + "\"");
                                    break;
                                }
                            }
                            for (int j = 0; j < defectFiles.size(); j++) {
                                writer.write("," + weightMatrix[i][j]);
                            }
                            writer.write("\r\n");
                        }
                        writer.close();

                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void MakeMaxForest(Path pathToDataset, Path pathToSaveRepresentations, String DefectA, String DefectB,
            String version) throws IOException {

        if (DefectA.equals(DefectB)) {
            System.out.println("Defect ID must be different for source (DefectA) and template (DefectB) ");
            return;
        }

        String badFolderName = pathToDataset.toString() + "\\bad";
        String goodFolderName = pathToDataset.toString() + "\\good";
        String checkSrc ="";

        List<String> defects = new ArrayList<String>();
        defects.add(DefectA);
        defects.add(DefectB);

        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() + "\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        try {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            var baseTime = System.currentTimeMillis();
            boolean isB = false;
            TreeContext srcA = null;
            TreeContext srcB= null;
            // TreeContext dstA = null;
            TreeContext dstB = null;
            // Matcher matcherA = null;
            // Matcher matcherB = null ;
            // List<Action> actA = null ;
            List<Action> actB = null;

            for (String fName : result) {
                Boolean useFile = false;
                for (String defect : defects) {
                    if (fName.contains(defect)) {
                        useFile = true;
                        if (defect == DefectA)
                            isB = false;
                        else
                            isB = true;
                        break;
                    }
                }
                if (useFile) {
                    try {

                        baseTime = System.currentTimeMillis();

                        Path methodBeforePath = Paths.get(fName);
                        Path methodAfterPath = Paths.get(fName.replace(badFolderName, goodFolderName));
                        String[] paths = splitPath(fName.replace(badFolderName, ""));

                        String defectId = paths[0] + "_" + version + "_" + paths[paths.length - 1];

                        System.out.println(getDiff(baseTime) + ": Defect id: " + defectId);

                        File fromFile = methodBeforePath.toFile();
                        File toFile = methodAfterPath.toFile();

                        File actionsFile = new File(pathToSaveRepresentations.toString() + "\\" + defectId);

                        //String rightSolutionId = defectId + "_" + OK.ordinal();
                        //String wrongSolutionId = defectId + "_" + FAIL.ordinal();

                        if (fromFile.length() > 0 && toFile.length() > 0) {
                            System.out.println("Sizes: " + fromFile.length() + " ->" + toFile.length());
                            // double rate = ((double) fromFile.length()) / ((double) toFile.length());
                            System.out.println(getDiff(baseTime) + ": Checking size");
                            if (fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE) {
                                {
                                    var fromCode = Files.readString(methodBeforePath);
                                    var toCode = Files.readString(methodAfterPath);

                                    if(!isB){
                                        checkSrc = toCode;
                                    }

                                    System.out.println(getDiff(baseTime) + ": Files loaded");

                                    var fromSolution = new Solution(fromCode, defectId, "bad_1", FAIL);
                                    var toSolution = new Solution(toCode, defectId, "good_1", OK);

                                    TreeContext src = null;
                                    TreeContext dst = null;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(new NamesASTNormalizer());
                                    } else {
                                        generator = new CachedASTGenerator(new BasicASTNormalizer());
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    
                                    
                                    // src = new srcmlGenerator().generateFromString(fromCode);

                                    System.out.println("SRC tree size=" + src.getRoot().getSize());

                                    TreeIoUtils.toXml(src)
                                            .writeTo(pathToSaveRepresentations.toString() + "\\ast\\src_" + defectId);

                                    if (isB) {

                                        dst = generator.buildTreeContext(toSolution);

                                        //dst =  new srcmlGenerator().generateFromString(toCode);

                                        System.out.println("DST tree size=" + dst.getRoot().getSize());
                                       

                                        Matcher matcherAst = Matchers.getInstance().getMatcher(src.getRoot(),
                                                dst.getRoot());
                                        System.out.println("Compare trees");
                                        try {
                                            matcherAst.match();
                                        } catch (NullPointerException e) {
                                            System.out.println(e.getMessage());
                                        }

                                        TreeIoUtils.toAnnotatedXml(dst, false,  matcherAst.getMappings()).writeTo(
                                            pathToSaveRepresentations.toString() + "\\ast\\dst_" + defectId);

                                        TreeIoUtils.toAnnotatedXml(src, true,  matcherAst.getMappings()).writeTo(
                                                pathToSaveRepresentations.toString() + "\\ast\\dst_" + defectId);    

                                        System.out.println("Build AST");

                                    

                                        ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(),
                                                dst.getRoot(), matcherAst.getMappings());
                                        try {
                                            actionGenerator.generate();
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                            e.printStackTrace();
                                        }

                                        final List<Action> actions = actionGenerator.getActions();

                                        srcB = src;
                                        dstB = dst;
                                        // matcherB =matcherAst;
                                        actB = actions;
                                    } else {
                                        srcA = src;
                                        // dstA = dst;
                                        // matcherA =matcherAst;
                                        // actA = actions;
                                        // isB = true;

                                    }

                                    //fromSolution = null;
                                    //toSolution = null;

                                }
                                System.out.println(getDiff(baseTime) + ": defect processed");
                            } else {
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " + defectId
                                        + " Very large file  size.");
                            }

                        }

                        toFile = null;
                        fromFile = null;

                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }
                }

            }


           

            System.out.println(getDiff(baseTime) + ": Try to build maxtree");

            if (srcA != null && dstB != null && actB != null && srcB != null) {

                if (actB.size() > 0) {
                    testMatcher matcher = new testMatcher(srcA.getRoot(), srcB.getRoot(), new MappingStore());

                    try {
                        matcher.match();
                    } catch (NullPointerException e) {
                        System.out.println(e.getMessage());

                    }

                
                    // try to get common forest instead common tree
                    List<ITree> forest = matcher.GetLongestForest(actB);
                    int forestIdx = 0;
                    for (ITree minSrc : forest) {
                        forestIdx++;
                        TreeContext mSrc = new TreeContext();
                        mSrc.importTypeLabels(dstB);
                        mSrc.setRoot(minSrc);
                        mSrc.getRoot().refresh();

                        try {
                            TreeIoUtils.toAnnotatedXml(mSrc,true,matcher.getMappings()).writeTo(pathToSaveRepresentations.toString() + "\\ast\\maxTree."
                                    + forestIdx + "_" + DefectA + "_to_" + DefectB + ".xml");

                            if(minSrc.getLength()>0){
                                BufferedWriter writer = new BufferedWriter(
                                    new FileWriter(pathToSaveRepresentations.toString() + "\\ast\\src."
                                    + forestIdx + "_" + DefectA + "_to_" + DefectB + ".txt"));
                                writer.write(checkSrc.substring( minSrc.getPos(), minSrc.getEndPos() ));
                                writer.close();

                            }
                            

                            

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                } else {
                    System.out.println("No edit actions for defect2. Tree will be empty");
                }
            }
            System.out.println(getDiff(baseTime) + ": Done");

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    public static void getTop10(Path pathToDataset, Path pathToListFile, Path pathToMatrix, String version, String Verbose)
            throws IOException {
        List<String> defects = Files.readAllLines(pathToListFile);

        List<String> defectFiles = new ArrayList<String>();
        List<String> defectIds = new ArrayList<String>();

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        if(Verbose.equals("yes")){
            File directory2 = new File(pathToMatrix.toString() +"\\src");
            if (!directory2.exists()) {
                directory2.mkdir();
            }


            File directory3 = new File(pathToMatrix.toString() +"\\tree");
            if (!directory3.exists()) {
                directory3.mkdir();
            }
        }

        String badFolderName = pathToDataset.toString() + "\\bad";
        String goodFolderName = pathToDataset.toString() + "\\good";

        try {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result) {
                boolean useFile = false;
                String defectId="";

                for (String defect : defects) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                            defectId = defect;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles.add(fName);
                    defectIds.add(defectId);
                }
            }

            // defect files is a collection of bad files
            if (defectFiles.size() > 0) {

                // collect common actions for cluster here
                int[][] weightMatrix = new int[defectFiles.size()][defectFiles.size()];

                ASTGenerator generator = null;

                if (version.toLowerCase().equals("abstract")) {
                    generator = new CachedASTGenerator(new NamesASTNormalizer());
                } else {
                    generator = new CachedASTGenerator(new BasicASTNormalizer());
                }

                for (int i = 0; i < defectFiles.size(); i++) {

                    String defectB = defectFiles.get(i);
                    TreeContext srcB = null;
                    TreeContext dstB = null;
                    List<Action> actB = null;
                    Matcher matcherAst =null;
                    String fromCode="";
                    String toCode="";

                    try {
                        fromCode = Files.readString(Paths.get(defectB));
                        toCode = Files.readString(Paths.get(defectB.replace(badFolderName, goodFolderName)));
                        if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                            var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                            var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                            srcB = generator.buildTreeContext(fromSolution);
                            dstB = generator.buildTreeContext(toSolution);

                            matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                            System.out.println("Compare trees");
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

                            fromSolution = null;
                            toSolution = null;
                        }
                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }

                    // calculate for file
                    for (int j = 0; j < defectFiles.size(); j++) {
                        System.out.println(">>>>" + i + " x " + j + "(" + defectFiles.size() + ")");
                        weightMatrix[i][j] = 0;
                        if (i != j) {
                            String defectA = defectFiles.get(j);
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

                                    if (actB.size() > 0) {
                                       // testMatcher matcher = new testMatcher(srcA.getRoot(), srcB.getRoot(),
                                       //         new MappingStore());

                                        testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),
                                                new MappingStore());
                                                

                                        try {
                                            matcher.match();
                                        } catch (NullPointerException e) {
                                            System.out.println(e.getMessage());
                                        }

                                        // ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                                        // weightMatrix[i][j] =minSrc.getSize();

                                        List<ITree> forest = matcher.GetLongestForest(actB);
                                        int forestIdx=0;
                                        for (ITree minSrc : forest) {
                                            weightMatrix[i][j] += minSrc.getSize();
                                            forestIdx++;



                                            if(Verbose.equals("yes")){
                                                if(minSrc.getLength()>0){
                                                    ITree nfa =matcher.GetNFA(minSrc.getId());
                                                    ITree nfaDst =null;
                                                    Integer up=0;

                                                    while (up <=5 && nfa!= null && nfa.getParent() != null){
                                                        nfa=nfa.getParent();
                                                        if(matcherAst.getMappings().getDst(nfa) != null){
                                                            nfaDst = matcherAst.getMappings().getDst(nfa);
                                                        }
                                                        up++;
                                                    }

                                                    BufferedWriter writer = new BufferedWriter(
                                                        new FileWriter(pathToMatrix.toString() + "\\src\\from_"
                                                        +  defectIds.get(i) + "_to_" +defectIds.get(j) + "_t_" + forestIdx  + ".txt"));
                                                    
                                                        if(nfa != null && nfa.getLength() >0){
                                                            writer.write("----------- Code from template source -----------------------------\r\n");
                                                            writer.write(fromCode.substring( nfa.getPos()-1, nfa.getEndPos()-1 ));    
                                                            writer.write("\r\n-------------------------------------------------------------------\r\n");
                                                        }

                                                        if(nfaDst != null && nfaDst.getLength() >0){
                                                            writer.write("----------- Code from template dest -----------------------------\r\n");
                                                            writer.write(toCode.substring( nfaDst.getPos()-1, nfaDst.getEndPos()-1 ));    
                                                            writer.write("\r\n-------------------------------------------------------------------\r\n");
                                                        }

                                                        writer.write("----------- Code from examinated file -----------------------------\r\n");
                                                        writer.write(fromCodeA.substring( minSrc.getPos()-1, minSrc.getEndPos()-1 ));
                                                        writer.write("\r\n-------------------------------------------------------------------\r\n");

                                                    writer.close();
                                                    

                                                
                                                    TreeContext mSrc = new TreeContext();
                                                    mSrc.importTypeLabels(dstB);
                                                    mSrc.setRoot(minSrc);
                                                    mSrc.getRoot().refresh();
                                                    TreeIoUtils.toXml(mSrc).writeTo(pathToMatrix.toString() + "\\tree\\from_"
                                                    +  defectIds.get(i) + "_to_" +defectIds.get(j) + "_t_" + forestIdx  + ".xml" );
                                                    
                                                }
                                            }
                                            

                                        }

                                        

                                    }
                                }

                            } catch (Exception any) {
                                System.out.println(any.getMessage());
                                any.printStackTrace();
                            }

                        }
                    }

                    // write csv result for given (each) defectB
                    {

                        int Size = 0, Cnt = 0;
                        String calcDefect = "";
                        for (int j = 0; j < defectFiles.size(); j++) {
                            if (weightMatrix[i][j] > 1)
                                Cnt++;
                            Size += weightMatrix[i][j];
                        }

                        for (String defect : defects) {
                            if (defectFiles.get(i).contains("\\" + defect + "\\")) {

                                calcDefect = defect;
                                break;
                            }
                        }

                        String matrixFile = pathToMatrix.toString() + "\\";
                        matrixFile += Cnt + "_" + Size + "_(" + calcDefect + ").csv";
                        BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));
                        writer.write("\"" + calcDefect + "\"," + Cnt + "," + Size + "\r\n");
                        {

                            writer.write("\"base\"");
                            for (int j = 0; j < defectFiles.size(); j++) {
                                for (String defect : defects) {
                                    if (defectFiles.get(j).contains("\\" + defect + "\\")) {
                                        writer.write(",\"" + defect + "\"");
                                        break;
                                    }
                                }
                            }
                            writer.write("\r\n\"" + calcDefect + "\"");
                            for (int j = 0; j < defectFiles.size(); j++) {
                                writer.write("," + weightMatrix[i][j]);
                            }
                            writer.write("\r\n");
                        }
                        writer.close();

                    }

                }

                // calculate tipical defects for set
                int[] baseSum = new int[defectFiles.size()];
                int[] baseCnt = new int[defectFiles.size()];
                for (int i = 0; i < defectFiles.size(); i++) {
                    baseSum[i] = 0;
                    baseCnt[i] = 0;

                    for (int j = 0; j < defectFiles.size(); j++) {
                        if (weightMatrix[i][j] > 1)
                            baseCnt[i]++;
                        baseSum[i] += weightMatrix[i][j];
                    }
                }

                String matrixFile = pathToMatrix.toString() + "\\top10.txt";
                BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                List<String> topN = new ArrayList<String>();
                while (topN.size() < 10) {
                    int maxIdx = 0;
                    for (int i = 0; i < defectFiles.size(); i++) {
                        if (baseCnt[i] > baseCnt[maxIdx])
                            maxIdx = i;
                        else if (baseCnt[i] == baseCnt[maxIdx]) {
                            if (baseSum[i] > baseSum[maxIdx])
                                maxIdx = i;
                        }

                    }
                    if (baseCnt[maxIdx] == 0)
                        break;
                    topN.add(defectFiles.get(maxIdx));
                    baseCnt[maxIdx] = 0;
                    baseSum[maxIdx] = 0;
                }

                for (int i = 0; i < topN.size(); i++) {
                    for (String defect : defects) {
                        if (topN.get(i).contains("\\" + defect + "\\")) {
                            writer.write(defect + "\r\n");
                        }
                    }
                }

                writer.write("\r\n");
                for (int i = 0; i < defectFiles.size(); i++) {

                    for (String defect : defects) {
                        if (defectFiles.get(i).contains("\\" + defect + "\\")) {
                            writer.write("[" + defect + "]");
                            break;
                        }
                    }
                    for (int j = 0; j < defectFiles.size(); j++) {
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

    public static void lookLike(Path pathToDataset1, Path pathToListFile1, Path pathToDataset2, Path pathToListFile2,
            Path pathToMatrix, String version) throws IOException {

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }


        File directory2 = new File(pathToMatrix.toString() +"\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }
        // first dataset - test (bad files only)
        List<String> defects1 = Files.readAllLines(pathToListFile1);
        List<String> defectFiles1 = new ArrayList<String>();

        String badFolderName1 = pathToDataset1.toString() + "\\bad";
        // String goodFolderName1 = pathToDataset1.toString() + "\\good";

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
                int[][] weightMatrix = new int[defectFiles2.size()][defectFiles1.size()];

                ASTGenerator generator = null;

                if (version.toLowerCase().equals("abstract")) {
                    generator = new CachedASTGenerator(new NamesASTNormalizer());
                } else {
                    generator = new CachedASTGenerator(new BasicASTNormalizer());
                }

                for (int i = 0; i < defectFiles2.size(); i++) {

                    // get template defects from second dataset
                    String defectB = defectFiles2.get(i);
                    TreeContext dstB = null;
                    List<Action> actB = null;
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
                            System.out.println("Compare trees");
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

                            fromSolution = null;
                            toSolution = null;
                        }
                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }

                    // scan all dataset 1 for test with template item from dataset 2
                    for (int j = 0; j < defectFiles1.size(); j++) {
                        System.out.println(">>>>" + i + "(" + defectFiles2.size() + ")" + " x " + j + "("
                                + defectFiles1.size() + ")");
                        weightMatrix[i][j] = 0;
                        if (i != j) {
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

                                    if (actB.size() > 0) {
                                        testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),
                                                new MappingStore());

                                        try {
                                            matcher.match();
                                        } catch (NullPointerException e) {
                                            System.out.println(e.getMessage());
                                        }

                                        List<ITree> forest = matcher.GetLongestForest(actB);

                                        int forestIdx = 0;
                                        for (ITree minSrc : forest) {
                                            forestIdx++;
                                            if(minSrc.getSize()>9){ 

                                                TreeContext mSrc = new TreeContext();
                                                mSrc.importTypeLabels(dstB);
                                                mSrc.setRoot(minSrc);
                                                mSrc.getRoot().refresh();
                        
                                                try {
                                                    TreeIoUtils.toXml(mSrc).writeTo(pathToMatrix.toString() + "\\ast\\maxTree."
                                                            + i + "_to_" + j + "_tree_" + forestIdx + ".xml");
                        

                                                } catch (Exception e) {
                                                    System.out.println(e.getMessage());
                                                }
                                                weightMatrix[i][j] += minSrc.getSize();
                                            }
                                        }




                                    }
                                }

                            } catch (Exception any) {
                                System.out.println(any.getMessage());
                                any.printStackTrace();
                            }

                        }
                    }

                    // write csv result for given (each) defectB
                    {

                        int Size = 0, Cnt = 0;
                        String calcDefect = "";
                        for (int j = 0; j < defectFiles1.size(); j++) {
                            if (weightMatrix[i][j] > 1)
                                Cnt++;
                            Size += weightMatrix[i][j];
                        }

                        for (String defect : defects1) {
                            if (defectFiles1.get(i).contains("\\" + defect + "\\")) {

                                calcDefect = defect;
                                break;
                            }
                        }

                        String matrixFile = pathToMatrix.toString() + "\\";
                        matrixFile += Cnt + "_" + Size + "_(" + calcDefect + ").csv";
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

}
