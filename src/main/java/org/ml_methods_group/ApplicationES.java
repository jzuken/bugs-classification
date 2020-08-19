package org.ml_methods_group;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.srcml.SrcmlCTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;
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



public class ApplicationES extends ApplicationMethods {
    public static void prepareESDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList, String version, int NgramSize) throws IOException {

        
        int processed=0;
        int skipped=0;
        int total=0;


        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   
        List<String> defects = Files.readAllLines(pathToBugList);                       
        try  {
            System.out.println("Build list of files");
            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
        
            System.out.println("Collected " +result.size() +" files");

            EditActionStore store = new EditActionStore();
            var baseTime = System.currentTimeMillis();

            for (String fName : result) {
                total++;
                if(total % 1000 ==0)
                    System.out.println("******************* total: " + total);
                Boolean useFile = false;
                for( String defect: defects) {
                    if(fName.contains(defect)){
                        useFile = true;
                        break;
                    }
                }
                if(useFile){
                    try{
                    
                    baseTime = System.currentTimeMillis();
                    processed++;
                    System.out.println("******************* total: " + total +", found: " + processed + ", skipped: " + skipped);

                    Path methodBeforePath = Paths.get(fName);
                    Path methodAfterPath = Paths.get(fName.replace(badFolderName, goodFolderName));
                    String[] paths = splitPath(fName.replace(badFolderName, ""));
                    
                    String defectId = paths[0]  +"_"+ version +"_" + paths[paths.length-1];

                    System.out.println(getDiff(baseTime) + ": Defect id: " +  defectId  );

                    File fromFile = methodBeforePath.toFile();
                    File toFile = methodAfterPath.toFile();

                    File actionsFile = new File(pathToSaveRepresentations.toString()+"\\" + defectId);
                    
                    String rightSolutionId = defectId + "_" + OK.ordinal();
                    String wrongSolutionId = defectId + "_" + FAIL.ordinal();


                    if(fromFile.length() >0 && toFile.length() >0 ){
                        System.out.println("Sizes: " + fromFile.length() +" ->" + toFile.length());
                        double rate = ((double) fromFile.length()) / ((double) toFile.length());
                        System.out.println(getDiff(baseTime) + ": Checking size");
                        if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE && rate >= 0.85 && rate <= 1.15){

                                System.out.println(getDiff(baseTime) + ": Rate: " + rate ); //+" Files before: " + methodBeforePath.toString() +", after: " + methodAfterPath.toString());
                                String emuCode = "";

                                if(actionsFile.exists()){
                                    System.out.println(getDiff(baseTime) + ": repared file exists");
                                }else{

                                    // write empty file for skip crash at next pass
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                    writer.write("{}");
                                    writer.close();

                                    var fromCode = Files.readString(methodBeforePath);
                                    var toCode = Files.readString(methodAfterPath);
                                
                                    
                                    System.out.println(getDiff(baseTime) + ": Files loaded");

                                    if(fromCode.length() >1000 || toCode.length() > 1000){
                                        String s1=EditActionStore.GetDifference(fromCode, toCode);
                                        String s2=EditActionStore.GetDifference(toCode, fromCode);
                                        //System.out.println("s1:" +s1);
                                        //System.out.println("s2:" +s2);
                                        fromCode =s1;
                                        toCode =s2;
                                    }
                                    


                                    var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                    var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                    
                                    System.out.println(getDiff(baseTime) + ": Building source actions");
                                    List<Action> actions = buildMethodActions(fromSolution, toSolution);
                                    System.out.println(getDiff(baseTime) + ": Buit source actions");

                                    fromSolution = null;
                                    toSolution = null;

                                    if(actions != null  && actions.size() > 0 ){
                                        System.out.println(getDiff(baseTime) + ": Prepare es");
                                        Pair<List<String>, List<String>> actionsStrings = store.convertToStrings(actions);

                                    
                                        actions= null;

                                        emuCode = "";
                                        int Cnt = 0;
                                        // store NGRAMS
                                        switch (version.toLowerCase()) {
                                            case "ngram": {
                                                List<BitSet> NGrams = store.calcActionsNgram(actionsStrings.getSecond(), NgramSize);
                                                //System.out.println("NGarms: " +NGrams.toString());
                                                for (BitSet bs : NGrams) {
                                                    String tmp = bs.toString();
                                                    if (!tmp.equals("{}")) {
                                                        Cnt++;
                                                        emuCode += "int x" + Cnt + "[] =" + tmp + ";\n";
                                                    }
                                                }
                                                NGrams = null;
                                            }
                                            break;


                                            case "textngram": {
                                                List<BitSet> NGrams = store.calcActionsNgram(actionsStrings.getSecond(), NgramSize);

                                                for (BitSet bs : NGrams) {
                                                    String tmp = store.NgramToText(bs);
                                                    Cnt++;
                                                    emuCode += "char* x" + Cnt + "[] =\"" + tmp + "\";\n";

                                                }

                                                NGrams = null;
                                            }
                                            break;

                                            case "code":
                                                for (String s : actionsStrings.getSecond()) {
                                                    emuCode += EditActionStore.actionToC(s) + ";\n";
                                                }
                                                break;

                                            case "bitset":

                                                for (String s : actionsStrings.getSecond()) {
                                                    if (!s.equals("")) {
                                                        Cnt++;
                                                        emuCode += "int x" + Cnt + "[] =" + store.calcActionsBitSet(s) + ";\n";
                                                    }
                                                }
                                                break;

                                        }

                                        actionsStrings = null;

                                        emuCode = "void block(){\n" + emuCode + "}\n";
                                        //System.out.println("emuCode: " + emuCode);

                                        writer =null;
                                        writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                        writer.write(emuCode);
                                        writer.close();

                                    }else{
                                        writer =null;
                                        System.out.println(getDiff(baseTime) + ": No actions detected");    
                                    }
                                }
                                System.out.println(getDiff(baseTime) + ": Done");
                            }else{
                                skipped++;
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " +  defectId +" Very large file difference or size. Rate: " + rate); // Files before: " + methodBeforePath.toString() +", after: " + methodAfterPath.toString());
                            }

                        }
                        
                        toFile = null;
                        fromFile = null;

                    }catch(Exception any)
                    {
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": All files are prepared");

            
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static void clusterESDataset(Path pathToPrepared, Path pathToSaveCluster, Path pathToBugList, String version,  ClusteringAlgorithm algorithm,
                                         double distanceLimit, int minClustersCount) throws IOException {

        int processed=0;
        int skipped=0;
        int total=0;
        List<Changes> AllChanges = new ArrayList();
        List<String> defects = Files.readAllLines(pathToBugList); 

                                
        try  {
            var baseTime = System.currentTimeMillis();
            Path clusterPath = Paths.get(pathToSaveCluster.toString() + "/cluster_" + version + "_" + algorithm.getCode() + ".txt");
            List<String> result = Files.walk(Paths.get(pathToPrepared.toString())).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
            for (String fName : result) {
                total++;
                Boolean useFile = false;
                String defectId ="";
                for( String defect: defects) {
                    if(fName.contains(defect)){
                        useFile = true;
                        defectId= defect;
                        break;
                    }
                }
                if(useFile){
                    try{
                        //String[] paths = splitPath(fName.replace(pathToPrepared.toString(), ""));
                        //String defectId =  paths[paths.length-1];
                        baseTime = System.currentTimeMillis();
                        processed++;
                        System.out.println("******************* total: " + total +", found: " + processed + ", skipped: " + skipped);
                        String emuCode = "";
                        System.out.println(getDiff(baseTime) + ":  " + defectId);
                        emuCode = Files.readString(Paths.get(fName));
                        if(! emuCode.equals("{}") ){
                            var fromSolutionNG = new Solution("", defectId, defectId+"_EMPTY", FAIL);
                            var toSolutionNG = new Solution(emuCode, defectId, defectId+"_ES", OK);
                            System.out.println(getDiff(baseTime) + ": Creating es changes (" + emuCode.length() +" bytes )");
                            Changes change = getChanges(false, fromSolutionNG, toSolutionNG);
                            System.out.println(getDiff(baseTime) + ": Collect es changes");
                            AllChanges.add(change);
                        }else{
                            skipped++;
                            System.out.println(getDiff(baseTime) + ": Skip no-action file");
                        }
    

                    }catch(Exception any)
                    {
                        any.printStackTrace();
                    }
                }

            }
            

            System.out.println(getDiff(baseTime) + ": All changes are processed, starting clustering");

            doClustering(clusterPath, baseTime, AllChanges, algorithm, distanceLimit, minClustersCount);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

  
   
}