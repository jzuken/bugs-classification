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
import java.nio.file.Path;package org.ml_methods_group;

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

public class ApplicationLASE extends ApplicationMethods {
    
    
    protected static List<String> BuildCommonActions(List<String> A, List<String> B){
        int maxCnt=0;
        int posMaxA =-1;
        for (int i=0; i< A.size(); i++) {
            for (int j=0; j< B.size(); j++) {
                int k=i;
                int l=j;
                int cnt=0;    
                while(k < A.size() && l < B.size() && A.get(k).equals(B.get(l)) ){
                        cnt++;
                        // System.out.println("A[" +k +"] = B["+ l +"], cnt=" + cnt);
                        k++;
                        l++;
                }
                
                if(cnt > maxCnt){
                    maxCnt = cnt;
                    posMaxA = i;
                    System.out.println("New max at " + i + " = "+ maxCnt);
                }
                
            }
        }
        List<String> result = new ArrayList<>();
        if(maxCnt >0){
            for (int i=0; i< maxCnt; i++) {
                result.add(A.get(posMaxA+i));
            }
        }
        System.out.println("result CA: " + result.size());

        return result;
    }
    
    
    public static void prepareLASEDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList, String version) throws IOException {

        int processed=0;
        int skipped=0;

        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   
        List<String> defects = Files.readAllLines(pathToBugList);        
        
        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString() );
        if(!directory.exists()){
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() +"\\ast" );
        if(!directory2.exists()){
            directory2.mkdir();
        }
       

        try  {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
        
       
            var baseTime = System.currentTimeMillis();

            for (String fName : result) {
                Boolean useFile =false;
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
                    System.out.println("******************* found: " + processed + ", skipped: " + skipped);

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
                        if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE &&  rate >= 0.85 && rate <= 1.15){

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
                                
                                    var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                    var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                    System.out.println(getDiff(baseTime) + ": Building source actions");

                                    TreeContext src;
                                    TreeContext dst;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                                    }else{
                                        generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    dst = generator.buildTreeContext(toSolution);
                                
                                    Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
                                    try {
                                        matcher.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                        
                                    }
                                    ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(), dst.getRoot(), matcher.getMappings());
                                    try{
                                        actionGenerator.generate();
                                    } catch (Exception e){
                                        System.out.println( e.getMessage());
                                        e.printStackTrace();
                                    }
                            
                                
                                    final List<Action> actions = actionGenerator.getActions();
                                    fromSolution = null;
                                    toSolution = null;

                                    if(actions != null  && actions.size() > 0 ){
                                        System.out.println(getDiff(baseTime) + ": Prepare es");
                                       

                                        emuCode = "";

                                        // store Actions
                                        // int idx=0;
                                           for (Action action : actions) { 
                                            ITree actNode =action.getNode();
                                            //ITree parent = actNode.getParent();
                                                String actString = action.getName()  ;
                                                //+ NodeType.valueOf( actNode.getType()).name() 
                                                //+ (actNode.hasLabel()? " " + actNode.getLabel().replace("\r"," ").replace("\n"," ") :"")
                                                //+ " to " + NodeType.valueOf( parent.getType()).name() ;

                                                //  use same path  for both methods ??? 
                                                if (version.toLowerCase().equals("abstract")) {
                                                    actString += " " + ActionContext.GetContextPath(action,false,src) + (actNode.hasLabel()? " " + actNode.getLabel().replace("\r"," ").replace("\n"," ") :"");
                                                }else{
                                                    actString += " " + ActionContext.GetContextPath(action,true,src) ;
                                                }

                                           //     idx++;
                                                TreeIoUtils.toXml(src ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\" + defectId );
                                                
                                                emuCode += actString +"\n";

                                        }
                                   
                                                                      

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
                        System.out.println( any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": All files are prepared");

            
        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

    }


    public static void MakeMaxTree(Path pathToDataset, Path pathToSaveRepresentations, String DefectA, String DefectB, String version) throws IOException {

        if( DefectA.equals(DefectB) ){ 
            System.out.println("Defect ID must be different for source (DefectA) and template (DefectB) ");
            return;
        }

        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   



        List<String> defects = new ArrayList<String>();        
        defects.add(DefectA);
        defects.add(DefectB);

        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString() );
        if(!directory.exists()){
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() +"\\ast" );
        if(!directory2.exists()){
            directory2.mkdir();
        }
       

        try  {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
        
       
            var baseTime = System.currentTimeMillis();
            boolean isB= false;
            TreeContext srcA = null;
            //TreeContext srcB= null;
            //TreeContext dstA = null;
            TreeContext dstB = null ;
            //Matcher matcherA = null;
            //Matcher matcherB = null ;
            //List<Action> actA = null ;
            List<Action> actB = null;



            for (String fName : result) {
                Boolean useFile =false;
                for( String defect: defects) {
                    if(fName.contains(defect)){
                        useFile = true;
                        if(defect == DefectA) 
                            isB=false;
                        else
                            isB=true;
                        break;
                    }
                }
                if(useFile){
                    try{
                    
                        baseTime = System.currentTimeMillis();
                    
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
                            //double rate = ((double) fromFile.length()) / ((double) toFile.length());
                            System.out.println(getDiff(baseTime) + ": Checking size");
                            if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE ){
                            {
                                var fromCode = Files.readString(methodBeforePath);
                                var toCode = Files.readString(methodAfterPath);

                                System.out.println(getDiff(baseTime) + ": Files loaded");
                            
                                var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                TreeContext src=null;
                                TreeContext dst=null;

                                ASTGenerator generator = null;

                                if (version.toLowerCase().equals("abstract")) {
                                    generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                                }else{
                                    generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                                }

                                src = generator.buildTreeContext(fromSolution);
                                System.out.println("SRC tree size=" + src.getRoot().getSize());

                                
                                
                                TreeIoUtils.toXml(src ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\src_" + defectId );
                                        
                                if(isB) {

                                    dst = generator.buildTreeContext(toSolution);
                                    System.out.println("DST tree size=" + dst.getRoot().getSize());
                                    TreeIoUtils.toXml(dst ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\dst_" + defectId );
                            
                                    Matcher matcherAst = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
                                    System.out.println("Compare trees");
                                    try {
                                        matcherAst.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                    }

                                    System.out.println("Build AST");

                                    ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(), dst.getRoot(), matcherAst.getMappings());
                                    try{
                                        actionGenerator.generate();
                                    } catch (Exception e){
                                        System.out.println( e.getMessage());
                                        e.printStackTrace();
                                    }
                            
                                
                                    final List<Action> actions = actionGenerator.getActions();

                                    //srcB = src;
                                    dstB = dst;
                                    // matcherB =matcherAst;
                                    actB = actions;
                                }else{
                                    srcA = src;
                                    //dstA = dst;
                                    //matcherA =matcherAst;
                                    //actA = actions;
                                    //isB = true;

                                }


                                fromSolution = null;
                                toSolution = null;

                                
                                }
                                System.out.println(getDiff(baseTime) + ": defect processed");
                            }else{
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " +  defectId +" Very large file  size." ); 
                            }

                        }
                        
                        toFile = null;
                        fromFile = null;

                    }catch(Exception any)
                    {
                        System.out.println( any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": Try to build maxtree");

            if(srcA != null && dstB != null  && actB !=null ){

                if(  actB.size() >0) {
                    testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),new MappingStore());
                                        
                    try {
                        matcher.match();
                    } catch (NullPointerException e) {
                        System.out.println(e.getMessage());
                        
                    }

                    ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                    
                    TreeContext mSrc = new TreeContext();
                    mSrc.importTypeLabels(dstB);
                    mSrc.setRoot(minSrc);
                    mSrc.getRoot().refresh();

                    try {
                        TreeIoUtils.toXml(mSrc ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\maxTree_" + DefectA + "_to_"+ DefectB + ".xml" );
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }else{
                    System.out.println("No edit actions for defect2. Tree will be empty");
                }
           }
           System.out.println(getDiff(baseTime) + ": Done");
           

            
        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

    }


    public static void buildLASEbyCluster(Path pathToLaseDataset, Path pathToClusterFile,  Path pathToCommonActions, String version) throws IOException {
        
        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum=0;

         // check directory structure
         File directory = new File(pathToCommonActions.toString() );
         if(!directory.exists()){
             directory.mkdir();
         }
                           
        try  {

            for(String cluster: clusters){
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum +")");

                if(defects.length >1){

                    List<String> result = Files.walk(pathToLaseDataset, 1).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
      
                    List<String> defectFiles = new ArrayList<>();


                    // collect all files for defect to build edit sequence
                    for (String fName : result) {
                        boolean useFile =false;
                        
                        for(String defect : defects){
                           
                            if(fName.contains(defect+"_"+version)){
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile){
                            String emuCode = Files.readString(Paths.get(fName));
                            if(! emuCode.equals("{}") ){
                                defectFiles.add(fName);
                               
                            }

                        }
                    }

                    boolean firstFile =true;

                    // collect common actions for cluster here
                    List<String> commonActions = new ArrayList<>();
                    
                    if(defectFiles.size() >0){
                      
                        for (String fName : defectFiles) {
                            System.out.println("processing file:" + fName);
                            List<String> actions = Files.readAllLines(Paths.get(fName));
                            if(actions.size() >0){

                                String[] paths = splitPath(fName);
                                
                                // build commoin Edit 
                                if(! firstFile){
                                    System.out.println("Sizes:" + commonActions.size() + " " + actions.size()); 
                                    commonActions = BuildCommonActions(commonActions, actions);

                                    // if no commonactions stop processing this cluster
                                    if(commonActions.size()==0){
                                        System.out.println("Stop analizing cluster:" + cluster + ". No common actions detected. " ); 
                                        break;
                                    }

                                   


                                }else{
                                    commonActions = actions;
                                    firstFile =false;
                                }
                                
                            }else{
                                System.out.println("skip file without edit actions: " + fName);
                            }
                            
                        } 
                        String CommonActionsName = pathToCommonActions.toAbsolutePath() + "/" + commonActions.size() + "_" + clusterNum +".txt";

                        BufferedWriter writer = new BufferedWriter(new FileWriter(CommonActionsName));
                        writer.write(cluster +"\r\n");
                        writer.write("_____________________________________________________\r\n");

                        for(String action: commonActions){
                            writer.write(action +"\r\n");
                        }
                        writer.close();
                    }
                }

            } 
                    
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void scanCluster(Path pathToDataset, Path pathToClusterFile,  Path pathToMatrix, String version) throws IOException {
        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum=0;

         // check directory structure
         File directory = new File(pathToMatrix.toString() );
         if(!directory.exists()){
             directory.mkdir();
         }
                           
        
        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   

        try  {

            for(String cluster: clusters){
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum +")");

                if(defects.length >1){

                    List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
      
                    List<String> defectFiles = new ArrayList<>();


                    // collect all files for defect to build matrix
                    for (String fName : result) {
                        boolean useFile =false;
                        
                        for(String defect : defects){
                           
                            if(fName.contains("\\"+defect+"\\")){
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile){
                            defectFiles.add(fName);
                        }
                    }
                   
                    // defect files is a colleaction of bad files
                    if(defectFiles.size() >0){
                   
                        // collect common actions for cluster here
                        int[][] weightMatrix = new int[defectFiles.size()][defectFiles.size()];

                        ASTGenerator generator = null;
                
                        if (version.toLowerCase().equals("abstract")) {
                            generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                        }else{
                            generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                        }


                        for(int i=0;i<defectFiles.size();i++){


                            String defectB = defectFiles.get(i);
                            TreeContext dstB = null ;
                            List<Action> actB = null;
                            try{
                                var fromCode = Files.readString(Paths.get(defectB));
                                var toCode = Files.readString(Paths.get(defectB.replace(badFolderName,goodFolderName)));
                                if(fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE ){

                                    var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                                    var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                                    TreeContext srcB=null;

                                    srcB = generator.buildTreeContext(fromSolution);
                                    dstB = generator.buildTreeContext(toSolution);
                                    
                                    Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                                    System.out.println("Compare trees");
                                    try {
                                        matcherAst.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                    }

                                    ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(), dstB.getRoot(), matcherAst.getMappings());
                                    try{
                                        actionGenerator.generate();
                                    } catch (Exception e){
                                        System.out.println( e.getMessage());
                                        e.printStackTrace();
                                    }
                                    
                                    actB = actionGenerator.getActions();
        
                                    fromSolution = null;
                                    toSolution = null;
                                }
                            }catch(Exception any){
                                System.out.println( any.getMessage());
                                any.printStackTrace();
                            }


                            for(int j=0;j<defectFiles.size();j++){
                                weightMatrix[i][j]=0;
                                if( i != j) {
                                    String defectA = defectFiles.get(j);
                                    TreeContext srcA = null;
                        
                                    try{
                                        srcA = null;
                                        var fromCodeA = Files.readString(Paths.get(defectA));
                                        if(fromCodeA.length() <= MAX_FILE_SIZE){
                                            var fromSolutionA = new Solution(fromCodeA, "A_BAD" , "A_BAD", FAIL);
                                            srcA = generator.buildTreeContext(fromSolutionA);
                                            fromSolutionA = null;
                                        }
      
                                        if(srcA != null && dstB != null  && actB !=null ){

                                            if(  actB.size() >0) {
                                                testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),new MappingStore());
                                                                    
                                                try {
                                                    matcher.match();
                                                } catch (NullPointerException e) {
                                                    System.out.println(e.getMessage());
                                                }
                            
                                                ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                                                weightMatrix[i][j] =minSrc.getSize();
                                            }
                                        }
                                        

                
                                    }catch(Exception any)
                                    {
                                        System.out.println( any.getMessage());
                                        any.printStackTrace();
                                    }

                                }
                            }
                        }
                    
                        // calculate tipical defect for cluster
                        int[] baseSum = new int[defectFiles.size()];
                        for(int i=0;i<defectFiles.size();i++){
                            baseSum[i]=0;

                            for(int j=0;j<defectFiles.size();j++){
                                if(weightMatrix[i][j] > 1 )
                                    baseSum[i]++;
                                    
                                //baseSum[i]+=weightMatrix[i][j];
                            }
                        }

                        String matrixFile =     pathToMatrix.toString() + "\\matrix_" + clusterNum +".csv";
                        BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                        writer.write("\"base\""  );
                        for(int j=0;j<defectFiles.size();j++){
                           
                            for(String defect : defects){
                                if(defectFiles.get(j).contains("\\"+defect+"\\")){
                                    writer.write(",\"" +defect +"\"");
                                    break;
                                }
                            }
                        }
                        writer.write("\r\n" );


                        int maxIdx=0;
                        for(int i=0;i<defectFiles.size();i++){
                            if(baseSum[i] > baseSum[maxIdx])
                                maxIdx = i;
                        }
                        for(int i=0;i<defectFiles.size();i++){
                            
                            
                            for(String defect : defects){
                                if(defectFiles.get(i).contains("\\"+defect+"\\")){
                                    if(i == maxIdx)
                                        writer.write("\"* " +defect +"\"");
                                    else
                                        writer.write("\"" +defect +"\"");
                                    break;
                                }
                            }
                            for(int j=0;j<defectFiles.size();j++){
                                writer.write("," + weightMatrix[i][j] );
                            }
                            writer.write("\r\n" );
                        }
                        writer.close();

                    }

                   
                }

            } 
                    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ApplicationLASE extends ApplicationMethods {
    
    
    protected static List<String> BuildCommonActions(List<String> A, List<String> B){
        int maxCnt=0;
        int posMaxA =-1;
        for (int i=0; i< A.size(); i++) {
            for (int j=0; j< B.size(); j++) {
                int k=i;
                int l=j;
                int cnt=0;    
                while(k < A.size() && l < B.size() && A.get(k).equals(B.get(l)) ){
                        cnt++;
                        // System.out.println("A[" +k +"] = B["+ l +"], cnt=" + cnt);
                        k++;
                        l++;
                }
                
                if(cnt > maxCnt){
                    maxCnt = cnt;
                    posMaxA = i;
                    System.out.println("New max at " + i + " = "+ maxCnt);
                }
                
            }
        }
        List<String> result = new ArrayList<>();
        if(maxCnt >0){
            for (int i=0; i< maxCnt; i++) {
                result.add(A.get(posMaxA+i));
            }
        }
        System.out.println("result CA: " + result.size());

        return result;
    }
    
    
    public static void prepareLASEDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList, String version) throws IOException {

        int processed=0;
        int skipped=0;

        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   
        List<String> defects = Files.readAllLines(pathToBugList);        
        
        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString() );
        if(!directory.exists()){
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() +"\\ast" );
        if(!directory2.exists()){
            directory2.mkdir();
        }
       

        try  {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
        
       
            var baseTime = System.currentTimeMillis();

            for (String fName : result) {
                Boolean useFile =false;
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
                    System.out.println("******************* found: " + processed + ", skipped: " + skipped);

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
                        if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE &&  rate >= 0.85 && rate <= 1.15){

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
                                
                                    var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                    var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                    System.out.println(getDiff(baseTime) + ": Building source actions");

                                    TreeContext src;
                                    TreeContext dst;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                                    }else{
                                        generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    dst = generator.buildTreeContext(toSolution);
                                
                                    Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
                                    try {
                                        matcher.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                        
                                    }
                                    ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(), dst.getRoot(), matcher.getMappings());
                                    try{
                                        actionGenerator.generate();
                                    } catch (Exception e){
                                        System.out.println( e.getMessage());
                                        e.printStackTrace();
                                    }
                            
                                
                                    final List<Action> actions = actionGenerator.getActions();
                                    fromSolution = null;
                                    toSolution = null;

                                    if(actions != null  && actions.size() > 0 ){
                                        System.out.println(getDiff(baseTime) + ": Prepare es");
                                       

                                        emuCode = "";

                                        // store Actions
                                        // int idx=0;
                                           for (Action action : actions) { 
                                            ITree actNode =action.getNode();
                                            //ITree parent = actNode.getParent();
                                                String actString = action.getName()  ;
                                                //+ NodeType.valueOf( actNode.getType()).name() 
                                                //+ (actNode.hasLabel()? " " + actNode.getLabel().replace("\r"," ").replace("\n"," ") :"")
                                                //+ " to " + NodeType.valueOf( parent.getType()).name() ;

                                                //  use same path  for both methods ??? 
                                                if (version.toLowerCase().equals("abstract")) {
                                                    actString += " " + ActionContext.GetContextPath(action,false,src) + (actNode.hasLabel()? " " + actNode.getLabel().replace("\r"," ").replace("\n"," ") :"");
                                                }else{
                                                    actString += " " + ActionContext.GetContextPath(action,true,src) ;
                                                }

                                           //     idx++;
                                                TreeIoUtils.toXml(src ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\" + defectId );
                                                
                                                emuCode += actString +"\n";

                                        }
                                   
                                                                      

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
                        System.out.println( any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": All files are prepared");

            
        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

    }


    public static void MakeMaxTree(Path pathToDataset, Path pathToSaveRepresentations, String DefectA, String DefectB, String version) throws IOException {

        if( DefectA.equals(DefectB) ){ 
            System.out.println("Defect ID must be different for source (DefectA) and template (DefectB) ");
            return;
        }

        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   



        List<String> defects = new ArrayList<String>();        
        defects.add(DefectA);
        defects.add(DefectB);

        // check directory structure
        File directory = new File(pathToSaveRepresentations.toString() );
        if(!directory.exists()){
            directory.mkdir();
        }

        File directory2 = new File(pathToSaveRepresentations.toString() +"\\ast" );
        if(!directory2.exists()){
            directory2.mkdir();
        }
       

        try  {

            List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
        
       
            var baseTime = System.currentTimeMillis();
            boolean isB= false;
            TreeContext srcA = null;
            //TreeContext srcB= null;
            //TreeContext dstA = null;
            TreeContext dstB = null ;
            //Matcher matcherA = null;
            //Matcher matcherB = null ;
            //List<Action> actA = null ;
            List<Action> actB = null;



            for (String fName : result) {
                Boolean useFile =false;
                for( String defect: defects) {
                    if(fName.contains(defect)){
                        useFile = true;
                        if(defect == DefectA) 
                            isB=false;
                        else
                            isB=true;
                        break;
                    }
                }
                if(useFile){
                    try{
                    
                        baseTime = System.currentTimeMillis();
                    
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
                            //double rate = ((double) fromFile.length()) / ((double) toFile.length());
                            System.out.println(getDiff(baseTime) + ": Checking size");
                            if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE ){
                            {
                                var fromCode = Files.readString(methodBeforePath);
                                var toCode = Files.readString(methodAfterPath);

                                System.out.println(getDiff(baseTime) + ": Files loaded");
                            
                                var fromSolution = new Solution(fromCode, defectId, wrongSolutionId, FAIL);
                                var toSolution = new Solution(toCode, defectId, rightSolutionId, OK);

                                TreeContext src=null;
                                TreeContext dst=null;

                                ASTGenerator generator = null;

                                if (version.toLowerCase().equals("abstract")) {
                                    generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                                }else{
                                    generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                                }

                                src = generator.buildTreeContext(fromSolution);
                                System.out.println("SRC tree size=" + src.getRoot().getSize());

                                
                                
                                TreeIoUtils.toXml(src ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\src_" + defectId );
                                        
                                if(isB) {

                                    dst = generator.buildTreeContext(toSolution);
                                    System.out.println("DST tree size=" + dst.getRoot().getSize());
                                    TreeIoUtils.toXml(dst ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\dst_" + defectId );
                            
                                    Matcher matcherAst = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
                                    System.out.println("Compare trees");
                                    try {
                                        matcherAst.match();
                                    } catch (NullPointerException e) {
                                        System.out.println(e.getMessage());
                                    }

                                    System.out.println("Build AST");

                                    ActionGenerator actionGenerator = new ActionGenerator(src.getRoot(), dst.getRoot(), matcherAst.getMappings());
                                    try{
                                        actionGenerator.generate();
                                    } catch (Exception e){
                                        System.out.println( e.getMessage());
                                        e.printStackTrace();
                                    }
                            
                                
                                    final List<Action> actions = actionGenerator.getActions();

                                    //srcB = src;
                                    dstB = dst;
                                    // matcherB =matcherAst;
                                    actB = actions;
                                }else{
                                    srcA = src;
                                    //dstA = dst;
                                    //matcherA =matcherAst;
                                    //actA = actions;
                                    //isB = true;

                                }


                                fromSolution = null;
                                toSolution = null;

                                
                                }
                                System.out.println(getDiff(baseTime) + ": defect processed");
                            }else{
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " +  defectId +" Very large file  size." ); 
                            }

                        }
                        
                        toFile = null;
                        fromFile = null;

                    }catch(Exception any)
                    {
                        System.out.println( any.getMessage());
                        any.printStackTrace();
                    }
                }

            }

            System.out.println(getDiff(baseTime) + ": Try to build maxtree");

            if(srcA != null && dstB != null  && actB !=null ){

                if(  actB.size() >0) {
                    testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),new MappingStore());
                                        
                    try {
                        matcher.match();
                    } catch (NullPointerException e) {
                        System.out.println(e.getMessage());
                        
                    }

                    ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                    
                    TreeContext mSrc = new TreeContext();
                    mSrc.importTypeLabels(dstB);
                    mSrc.setRoot(minSrc);
                    mSrc.getRoot().refresh();

                    try {
                        TreeIoUtils.toXml(mSrc ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\maxTree_" + DefectA + "_to_"+ DefectB + ".xml" );
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }else{
                    System.out.println("No edit actions for defect2. Tree will be empty");
                }
           }
           System.out.println(getDiff(baseTime) + ": Done");
           

            
        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

    }


    public static void buildLASEbyCluster(Path pathToLaseDataset, Path pathToClusterFile,  Path pathToCommonActions, String version) throws IOException {
        
        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum=0;

         // check directory structure
         File directory = new File(pathToCommonActions.toString() );
         if(!directory.exists()){
             directory.mkdir();
         }
                           
        try  {

            for(String cluster: clusters){
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum +")");

                if(defects.length >1){

                    List<String> result = Files.walk(pathToLaseDataset, 1).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
      
                    List<String> defectFiles = new ArrayList<>();


                    // collect all files for defect to build edit sequence
                    for (String fName : result) {
                        boolean useFile =false;
                        
                        for(String defect : defects){
                           
                            if(fName.contains(defect+"_"+version)){
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile){
                            String emuCode = Files.readString(Paths.get(fName));
                            if(! emuCode.equals("{}") ){
                                defectFiles.add(fName);
                               
                            }

                        }
                    }

                    boolean firstFile =true;

                    // collect common actions for cluster here
                    List<String> commonActions = new ArrayList<>();
                    
                    if(defectFiles.size() >0){
                      
                        for (String fName : defectFiles) {
                            System.out.println("processing file:" + fName);
                            List<String> actions = Files.readAllLines(Paths.get(fName));
                            if(actions.size() >0){

                                String[] paths = splitPath(fName);
                                
                                // build commoin Edit 
                                if(! firstFile){
                                    System.out.println("Sizes:" + commonActions.size() + " " + actions.size()); 
                                    commonActions = BuildCommonActions(commonActions, actions);

                                    // if no commonactions stop processing this cluster
                                    if(commonActions.size()==0){
                                        System.out.println("Stop analizing cluster:" + cluster + ". No common actions detected. " ); 
                                        break;
                                    }

                                   


                                }else{
                                    commonActions = actions;
                                    firstFile =false;
                                }
                                
                            }else{
                                System.out.println("skip file without edit actions: " + fName);
                            }
                            
                        } 
                        String CommonActionsName = pathToCommonActions.toAbsolutePath() + "/" + commonActions.size() + "_" + clusterNum +".txt";

                        BufferedWriter writer = new BufferedWriter(new FileWriter(CommonActionsName));
                        writer.write(cluster +"\r\n");
                        writer.write("_____________________________________________________\r\n");

                        for(String action: commonActions){
                            writer.write(action +"\r\n");
                        }
                        writer.close();
                    }
                }

            } 
                    
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void scanCluster(Path pathToDataset, Path pathToClusterFile,  Path pathToMatrix, String version) throws IOException {
        List<String> clusters = Files.readAllLines(pathToClusterFile);
        int clusterNum=0;

         // check directory structure
         File directory = new File(pathToMatrix.toString() );
         if(!directory.exists()){
             directory.mkdir();
         }
                           
        
        String badFolderName =  pathToDataset.toString() + "\\bad";                          
        String goodFolderName =  pathToDataset.toString() + "\\good";   

        try  {

            for(String cluster: clusters){
                String[] defects = cluster.split(" ");
                clusterNum++;
                System.out.println("processing cluster:" + cluster + " (" + clusterNum +")");

                if(defects.length >1){

                    List<String> result = Files.walk(Paths.get(badFolderName)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
      
                    List<String> defectFiles = new ArrayList<>();


                    // collect all files for defect to build matrix
                    for (String fName : result) {
                        boolean useFile =false;
                        
                        for(String defect : defects){
                           
                            if(fName.contains("\\"+defect+"\\")){
                                useFile = true;
                                break;
                            }
                        }
                        if (useFile){
                            defectFiles.add(fName);
                        }
                    }
                   
                    // defect files is a colleaction of bad files
                    if(defectFiles.size() >0){
                   
                        // collect common actions for cluster here
                        int[][] weightMatrix = new int[defectFiles.size()][defectFiles.size()];

                        ASTGenerator generator = null;
                
                        if (version.toLowerCase().equals("abstract")) {
                            generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                        }else{
                            generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                        }


                        for(int i=0;i<defectFiles.size();i++){


                            String defectB = defectFiles.get(i);
                            TreeContext dstB = null ;
                            List<Action> actB = null;
                            try{
                                var fromCode = Files.readString(Paths.get(defectB));
                                var toCode = Files.readString(Paths.get(defectB.replace(badFolderName,goodFolderName)));

                                var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                                var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                                TreeContext srcB=null;

                                srcB = generator.buildTreeContext(fromSolution);
                                dstB = generator.buildTreeContext(toSolution);
                                
                                Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                                System.out.println("Compare trees");
                                try {
                                    matcherAst.match();
                                } catch (NullPointerException e) {
                                    System.out.println(e.getMessage());
                                }

                                ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(), dstB.getRoot(), matcherAst.getMappings());
                                try{
                                    actionGenerator.generate();
                                } catch (Exception e){
                                    System.out.println( e.getMessage());
                                    e.printStackTrace();
                                }
                                
                                actB = actionGenerator.getActions();
    
                                fromSolution = null;
                                toSolution = null;
                            }catch(Exception any){
                                System.out.println( any.getMessage());
                                any.printStackTrace();
                            }


                            for(int j=0;j<defectFiles.size();j++){
                                weightMatrix[i][j]=0;
                                if( i != j) {
                                    String defectA = defectFiles.get(j);
                                    TreeContext srcA = null;
                        
                                    try{
                                        var fromCodeA = Files.readString(Paths.get(defectA));
                                        var fromSolutionA = new Solution(fromCodeA, "A_BAD" , "A_BAD", FAIL);
                                        srcA = generator.buildTreeContext(fromSolutionA);
                                        fromSolutionA = null;
      
                                        if(srcA != null && dstB != null  && actB !=null ){

                                            if(  actB.size() >0) {
                                                testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),new MappingStore());
                                                                    
                                                try {
                                                    matcher.match();
                                                } catch (NullPointerException e) {
                                                    System.out.println(e.getMessage());
                                                }
                            
                                                ITree minSrc = matcher.GetLongestSrcSubtree(actB);
                                                weightMatrix[i][j] =minSrc.getSize();
                                            }
                                        }
                                        

                
                                    }catch(Exception any)
                                    {
                                        System.out.println( any.getMessage());
                                        any.printStackTrace();
                                    }

                                }
                            }
                        }
                    
                        // calculate tipical defect for cluster
                        int[] baseSum = new int[defectFiles.size()];
                        for(int i=0;i<defectFiles.size();i++){
                            baseSum[i]=0;

                            for(int j=0;j<defectFiles.size();j++){
                                if(weightMatrix[i][j] > 1 )
                                    baseSum[i]++;
                                    
                                //baseSum[i]+=weightMatrix[i][j];
                            }
                        }

                        String matrixFile =     pathToMatrix.toString() + "\\matrix_" + clusterNum +".csv";
                        BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

                        writer.write("\"base\""  );
                        for(int j=0;j<defectFiles.size();j++){
                           
                            for(String defect : defects){
                                if(defectFiles.get(j).contains("\\"+defect+"\\")){
                                    writer.write(",\"" +defect +"\"");
                                    break;
                                }
                            }
                        }
                        writer.write("\r\n" );


                        int maxIdx=0;
                        for(int i=0;i<defectFiles.size();i++){
                            if(baseSum[i] > baseSum[maxIdx])
                                maxIdx = i;
                        }
                        for(int i=0;i<defectFiles.size();i++){
                            
                            
                            for(String defect : defects){
                                if(defectFiles.get(i).contains("\\"+defect+"\\")){
                                    if(i == maxIdx)
                                        writer.write("\"* " +defect +"\"");
                                    else
                                        writer.write("\"" +defect +"\"");
                                    break;
                                }
                            }
                            for(int j=0;j<defectFiles.size();j++){
                                writer.write("," + weightMatrix[i][j] );
                            }
                            writer.write("\r\n" );
                        }
                        writer.close();

                    }

                   
                }

            } 
                    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
