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
import org.ml_methods_group.common.ast.srcmlGenerator;
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
import org.ml_methods_group.common.ast.matches.testStringAlgoritm;
import org.eclipse.jdt.core.jdom.IDOMCompilationUnit;
import org.ml_methods_group.ApplicationMethods;
import org.ml_methods_group.clustering.clusterers.HAC;
import org.ml_methods_group.CodeDescription;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import au.com.bytecode.opencsv.CSVReader;

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
                        var f = new File(fName);
                        if(f.length() <= MAX_FILE_SIZE)
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


    public static void prepareDescriptions(Path PathToDescriptionsCSV, Path pathToSaveRepresentations) throws IOException {

        
        File directory2 = new File(pathToSaveRepresentations.toString() );
        if (!directory2.exists()) {
            directory2.mkdir();
        }

        int processed=0;
        int skipped=0;
        int total=0;

        CSVReader reader = new CSVReader(new FileReader(PathToDescriptionsCSV.toString()), ',', '"', 1);
        //Read all rows at once
        List<String[]> allRows = reader.readAll();
        String [] StopWords ={"this","is", "a", "the","are","was","were","be","been","an","these", 
        "what","where","can","must","on","it","of","in","to","do","does","did","done","dosen","t",
        "by","resulting","will","all", "and", "not","when", "that","than","but","with","into","there", "so", "s"};
        List<String> StopList = Arrays.asList(StopWords);
        
        Map<String,String> Descriptions = new HashMap<String,String>();
        for(String d[] : allRows){
            if(d.length > 1)
                Descriptions.put(d[0],d[1]);
        }

                     
        try  {
            
            var baseTime = System.currentTimeMillis();

            for (String ID : Descriptions.keySet()) {
                total++;
                if(total % 1000 ==0)
                    System.out.println("******************* total: " + total);
                Boolean useFile = true;
                if(useFile){
                    try{
                    
                    baseTime = System.currentTimeMillis();
                    processed++;
                    System.out.println("******************* total: " + total +", found: " + processed + ", skipped: " + skipped);
                    String defectId = ID;
                    System.out.println(getDiff(baseTime) + ": Defect id: " +  defectId  );
                    File actionsFile = new File(pathToSaveRepresentations.toString()+"\\" + defectId+".c");
                    String data = Descriptions.get(ID);
                    if(data.length() >0  ){
                        
                                String emuCode = "";

                               
                                    System.out.println(getDiff(baseTime) + ": Prepare CodeDescription");
                                        data = data.replace("\"", " ");
                                        data = data.replace("\'", " ");
                                        data = data.replace("\n", " ");
                                        data = data.replace("\r", " ");
                                        data = data.replace("\t", " ");
                                        data = data.replace(".", " ");
                                        data = data.replace("{", " ");
                                        data = data.replace("}", " ");
                                        data = data.replace("(", " ");
                                        data = data.replace(")", " ");
                                        data = data.replace("[", " ");
                                        data = data.replace("]", " ");
                                        data = data.replace(",", " ");
                                        data = data.replace(";", " ");
                                        data = data.replace(":", " ");
                                        data = data.replace("=", " ");

                                        while(data.indexOf("  ") >=0){
                                            data = data.replace("  ", " ");
                                        }
                                        String words[] = data.split(" ");
                                        emuCode="";
                                        for(String s : words){
                                             if(! StopList.contains(s.toLowerCase()))
                                                emuCode += s.toLowerCase() +"\n";
                                        }

                                        //emuCode = "void block(){\n" + emuCode + "}\n";
                                        //System.out.println("emuCode: " + emuCode);

                                        BufferedWriter writer =null;
                                        writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                        writer.write(emuCode);
                                        writer.close();
                                }
                                System.out.println(getDiff(baseTime) + ": Done");
                     


                    }catch(Exception any)
                    {
                        any.printStackTrace();
                    }
                }

            }
            System.out.println(getDiff(baseTime) + ": All files CodeDescription prepared");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

   





    public static void clusterDescDataset(Path pathToPrepared, Path pathToSaveCluster, Path pathToBugList, String version,  
    double distanceLimit, int minClustersCount) throws Exception {

        int processed=0;
        int skipped=0;
        int total=0;

        List<CodeDescription> AllChanges = new ArrayList<CodeDescription> ();
        List<String> defects = Files.readAllLines(pathToBugList); 

        File directory2 = new File(pathToSaveCluster.toString() );
        if (!directory2.exists()) {
            directory2.mkdir();
        }


        try  {
            var baseTime = System.currentTimeMillis();
            Path clusterPath = Paths.get(pathToSaveCluster.toString() + "/cluster_" + version + ".txt");
            List<String> result = Files.walk(Paths.get(pathToPrepared.toString())).filter(Files::isRegularFile)
            .map(x -> x.toString()).collect(Collectors.toList());
            for (String fName : result) {
                total++;
                Boolean useFile = false;
                String defectId ="";
                for( String defect: defects) {
                    if(fName.contains(defect)){
                        var f = new File(fName);
                        if(f.length() <= MAX_FILE_SIZE){
                            useFile = true;
                            defectId= defect;
                        }
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
                            CodeDescription d = new CodeDescription(defectId,emuCode);
                            AllChanges.add ( d );
                        }else{
                            skipped++;
                            System.out.println(getDiff(baseTime) + ": Skip no-action file");
                        }


                    }catch(Exception any){
                        any.printStackTrace();
                    }
                }
            }   

        
            System.out.println(getDiff(baseTime) + ": All changes are processed, starting clustering");

             HAC<CodeDescription> h = new HAC<CodeDescription>(distanceLimit,minClustersCount,  new wordDistance());

             Clusters<CodeDescription> cd = h.buildClusters(AllChanges);

             System.out.println(getDiff(baseTime) + ": Clusters are formed, saving results");
             
             Clusters<String> idClusters = cd.map(x -> x.ID);

             FileOutputStream fileStream = new FileOutputStream(clusterPath.toFile(), false);
     
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileStream));
     
             for (var cluster : idClusters.getClusters()) {
                 bw.write(String.join(" ", cluster.getElements()));
                 bw.newLine();
             }
     
             bw.close();

             System.out.println(getDiff(baseTime) + ": Finished");

            
        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    public static void clusterESDataset(Path pathToPrepared, Path pathToSaveCluster, Path pathToBugList, String version,  ClusteringAlgorithm algorithm,
                                         double distanceLimit, int minClustersCount) throws IOException {

        int processed=0;
        int skipped=0;
        int total=0;
        List<Changes> AllChanges = new ArrayList<Changes>();
        List<String> defects = Files.readAllLines(pathToBugList); 

        File directory2 = new File(pathToSaveCluster.toString() );
        if (!directory2.exists()) {
            directory2.mkdir();
        }

                                
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
                        var f = new File(fName);
                        if(f.length() <= MAX_FILE_SIZE){
                            useFile = true;
                            defectId= defect;
                        }
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
                            var fromSolutionNG = new Solution("void block(){\n}\n", defectId, defectId, FAIL);
                            var toSolutionNG = new Solution(emuCode, defectId, defectId+"_ok",OK);
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


   


    private static class wordDistance implements DistanceFunction<CodeDescription> {

        private static final long serialVersionUID = 1L;
    
        @Override
        public double distance(CodeDescription first, CodeDescription second) {
            return distance(first, second, Double.POSITIVE_INFINITY);
        }
    
        @Override
        public double distance(CodeDescription s0, CodeDescription s1, double upperBound) {
            int maxLen=0;
            String commonWords="";
            /* 
            int[][] lengths = new int[s0.words.size() + 1][s1.words.size() + 1];
            for (int i = 0; i < s0.words.size(); i++){
                for (int j = 0; j < s1.words.size(); j++){
                    if (s0.words.get(i).equals(s1.words.get(j)))    
                        lengths[i + 1][j + 1] = lengths[i][j] + 1;
                    if(lengths[i + 1][j + 1] >maxLen)
                        maxLen=lengths[i + 1][j + 1];
                }
            }
            */

            for (int i = 0; i < s0.words.size(); i++){
                for (int j = 0; j < s1.words.size(); j++){
                    if (s0.words.get(i).equals(s1.words.get(j))){
                        maxLen++;
                        commonWords+=(s0.words.get(i)+" ");
                        break;
                    }    
                }
            }
            
            double dist = 1.0 - (double) maxLen / Math.max(Math.min(s0.words.size(),s1.words.size()),1);
            System.out.println(s0.ID + " x " + s1.ID + " -> " + maxLen +" d=" + dist + " [" +commonWords +"]");
            return dist >= upperBound ? upperBound : dist;
        }
    }
   
}
