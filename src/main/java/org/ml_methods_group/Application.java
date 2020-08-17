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


public class Application {

    static final double DEFAULT_DISTANCE_LIMIT = 0.3;
    static final int DEFAULT_MIN_CLUSTERS_COUNT = 1;
    static final int DEFAULT_NGRAM_SIZE = 5;
    static final int MAX_FILE_SIZE = 200 * 1024;
    static final ClusteringAlgorithm DEFAULT_ALGORITHM = ClusteringAlgorithm.BagOfWords;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Command expected: create, parse, cluster, clusterFolder or mark");
            return;
        }
        switch (args[0]) {
            case "create":
                if (args.length < 5 || args.length > 6 || args.length == 6 && !Arrays.asList(args).contains("--rename")) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Problem id" + System.lineSeparator() +
                            "    Path to code file before changes" + System.lineSeparator() +
                            "    Path to code file after changes" + System.lineSeparator() +
                            "    Path to file to store edit script" + System.lineSeparator() +
                            "    [Optional] --rename to rename variable names to generic names, default - false" + System.lineSeparator());
                    return;
                }
                createEditScript(
                        args[1],
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        Paths.get(args[4]),
                        Arrays.stream(args).map(String::toLowerCase).collect(Collectors.toList()).contains("--rename"));
                break;

            case "make.es":
                if (args.length < 4 || args.length > 5) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Problem id" + System.lineSeparator() +
                            "    Path to code file before changes" + System.lineSeparator() +
                            "    Path to code file after changes" + System.lineSeparator() +
                            "    Path to file to store edit script" + System.lineSeparator());
                    return;
                }
                makeEditScript(
                        args[1],
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        Paths.get(args[4])
                );
                break;

            case "prepare.es":
                if (args.length < 4 || args.length > 6) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code dataset" + System.lineSeparator() +
                            "    Path to store representation" + System.lineSeparator() +
                            "    Path to list of defect file" + System.lineSeparator() +
                            "    ES variant (code,  bitset, ngram, textngram)" + System.lineSeparator() +
                            "    [Optional] --ngramsize=X - Set size of ngram to X, default value 5" + System.lineSeparator());
                    return;
                }
                prepareESDataset(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4],
                        getNgramSizeFromArgs(args)

                );
                break;

            case "cluster.es":
                if (args.length < 3 || args.length > 6) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code prepared set" + System.lineSeparator() +
                            "    Path to store clustering" + System.lineSeparator() +
                            "    Path to list of defect file" + System.lineSeparator() +
                            "    [Optional] --algorithm=X - Set clusterization algorithm (bow, vec, jac, ext_jac, full_jac, fuz_jac), default value bow" + System.lineSeparator() +
                            "    [Optional] --distanceLimit=X - Set clustering distance limit to X, default value 0.3" + System.lineSeparator() +
                            "    [Optional] --minClustersCount=X - Set minimum amount of clusters to X, default value 1" + System.lineSeparator() 
                           );
                    return;
                }
                clusterESDataset(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4],
                        getAlgorithmFromArgs(args),
                        getDistanceLimitFromArgs(args),
                        getMinClustersCountFromArgs(args)

                );
                break;
            case "build.lase":
                if (args.length != 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to lase dataset" + System.lineSeparator() +
                            "    Path to cluster list file" + System.lineSeparator() +
                            "    Path to store cluster common actions" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                buildLASEbyCluster(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4]
                );
                break;

                case "prepare.lase":
                if (args.length != 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code dataset" + System.lineSeparator() +
                            "    Path to store representation" + System.lineSeparator() +
                            "    Path to list of defect file" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                prepareLASEDataset(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4]
                );
                break;

                case "make.maxtree":
                if (args.length != 6 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code dataset" + System.lineSeparator() +
                            "    Path to store representation" + System.lineSeparator() +
                            "    Path defect A id" + System.lineSeparator() +
                            "    Path defect B id" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                MakeMaxTree(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        args[3],
                        args[4],
                        args[5]
                );
                break;

            case "parse":
                if (args.length != 3) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to .csv file with submissions" + System.lineSeparator() +
                            "    Path to file to store parsed solutions" + System.lineSeparator());
                    return;
                }
                parse(Paths.get(args[1]), Paths.get(args[2]));
                break;
            case "clusterFolder":
                if (args.length < 3 || args.length > 6) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to folder with edit scripts" + System.lineSeparator() +
                            "    Path to file to store clusters" + System.lineSeparator() +
                            "    [Optional] --algorithm=X - Set clusterization algorithm (bow, vec, jac, ext_jac, full_jac, fuz_jac), default value bow" + System.lineSeparator() +
                            "    [Optional] --distanceLimit=X - Set clustering distance limit to X, default value 0.3" + System.lineSeparator() +
                            "    [Optional] --minClustersCount=X - Set minimum amount of clusters to X, default value 1" + System.lineSeparator()

                    );
                    return;
                }
                clusterFolder(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        getAlgorithmFromArgs(args),
                        getDistanceLimitFromArgs(args),
                        getMinClustersCountFromArgs(args));
                break;
            case "cluster":
                if (args.length < 3 || args.length > 8) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file which store parsed solutions" + System.lineSeparator() +
                            "    Path to file to store clusters" + System.lineSeparator() +
                            "    [Optional] --parallel to execute changes generation in parallel, default - false" + System.lineSeparator() +
                            "    [Optional] --rename to rename variable names to generic names, default - false" + System.lineSeparator() +
                            "    [Optional] --algorithm=X - Set clusterization algorithm (bow, vec, jac, ext_jac, full_jac, fuz_jac), default value bow" + System.lineSeparator() +
                            "    [Optional] --distanceLimit=X - Set clustering distance limit to X, default value 0.3" + System.lineSeparator() +
                            "    [Optional] --minClustersCount=X - Set minimum amount of clusters to X, default value 1" + System.lineSeparator());
                    return;
                }

                cluster(Paths.get(args[1]), Paths.get(args[2]),
                        getAlgorithmFromArgs(args),
                        getDistanceLimitFromArgs(args),
                        getMinClustersCountFromArgs(args),
                        Arrays.stream(args).map(String::toLowerCase).collect(Collectors.toList()).contains("--parallel"),
                        Arrays.stream(args).map(String::toLowerCase).collect(Collectors.toList()).contains("--rename"));
                break;
            case "mark":
                if (args.length != 5) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file which store clusters" + System.lineSeparator() +
                            "    Path to file to store marked clusters" + System.lineSeparator() +
                            "    Number of examples to show" + System.lineSeparator() +
                            "    Number of clusters to mark" + System.lineSeparator());
                    return;
                }
                mark(Paths.get(args[1]), Paths.get(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                break;
            case "prepare":
                if (args.length != 4) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file which store marks" + System.lineSeparator() +
                            "    Path to file which store marks parsed solutions" + System.lineSeparator() +
                            "    Path to file to store prepared marked data" + System.lineSeparator());
                    return;
                }
                prepare(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
                break;
            default:
                System.out.println("Undefined command!");
        }
    }

    private static double getDistanceLimitFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith("--distancelimit")).findFirst();

        if (param.isEmpty()) {
            return DEFAULT_DISTANCE_LIMIT;
        }

        return Double.parseDouble(param.get().toLowerCase().replace("--distancelimit=", ""));
    }

    private static int getMinClustersCountFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith("--minclusterscount")).findFirst();

        if (param.isEmpty()) {
            return DEFAULT_MIN_CLUSTERS_COUNT;
        }

        return Integer.parseInt(param.get().toLowerCase().replace("--minclusterscount=", ""));
    }

    private static int getNgramSizeFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith("--ngramsize")).findFirst();

        if (param.isEmpty()) {
            return DEFAULT_NGRAM_SIZE;
        }

        return Integer.parseInt(param.get().toLowerCase().replace("--ngramsize=", ""));
    }
    
    private static ClusteringAlgorithm getAlgorithmFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith("--algorithm")).findFirst();

        if (param.isEmpty()) {
            return DEFAULT_ALGORITHM;
        }

        return ClusteringAlgorithm.getAlgorithmByCode(
                param.get().toLowerCase().replace("--algorithm=", ""));
    }

    public static void parse(Path data, Path storage) throws IOException {
        try (InputStream input = new FileInputStream(data.toFile())) {
            final Dataset dataset = ParsingUtils.parse(input);
            ProtobufSerializationUtils.storeDataset(dataset, storage);
        }
    }

    public static void createEditScript(String id, Path fromFile, Path toFile, Path outputFile, boolean rename) throws IOException {
        final var fromCode = Files.readString(fromFile);
        final String wrongSolutionId = id + FAIL.ordinal();
        final var fromSolution = new Solution(fromCode, id, wrongSolutionId, FAIL);

        final var toCode = Files.readString(toFile);
        final String rightSolutionId = id + OK.ordinal();
        final var toSolution = new Solution(toCode, id, rightSolutionId, OK);

        final Changes change = getChanges(rename, fromSolution, toSolution);


        var start = System.currentTimeMillis();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                new FileOutputStream(outputFile.toString()));

        objectOutputStream.writeObject(change);
        objectOutputStream.close();

        System.out.println("Saving changes took " + ((System.currentTimeMillis() - start) / 1000.0) + " s");
    }

    private static ITree buildTree(Solution s) {
        ITree tree = null;
        //TreeContext context = null;
        try {
            final ASTGenerator generator = new BasicASTGenerator(new BasicASTNormalizer());
            tree = generator.buildTree(s);
            //tree = context.getRoot();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return tree;
    }

    public static List<Action> buildMethodActions(Solution FileBefore, Solution FileAfter)
            throws IOException {
        ITree src;
        ITree dst;
        try {
            final ASTGenerator generator = new BasicASTGenerator(new BasicASTNormalizer());
            src = generator.buildTree(FileBefore);
            dst = generator.buildTree(FileAfter);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        Matcher matcher = Matchers.getInstance().getMatcher(src, dst);
        try {
            matcher.match();
        } catch (NullPointerException e) {
            System.out.println("Cannot match: NullPointerException in m.match()");
            return null;
        }
        ActionGenerator generator = new ActionGenerator(src, dst, matcher.getMappings());
        try{
            generator.generate();
        } catch (Exception e){
            System.out.println("Generator Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return generator.getActions();
    }

    public static void makeEditScript(String id, Path fromFile, Path toFile, Path outputFile) throws IOException {
        final var fromCode = Files.readString(fromFile);
        final String wrongSolutionId = id + FAIL.ordinal();
        final var fromSolution = new Solution(fromCode, id, wrongSolutionId, FAIL);

        final var toCode = Files.readString(toFile);
        final String rightSolutionId = id + OK.ordinal();
        final var toSolution = new Solution(toCode, id, rightSolutionId, OK);

        final EditActions ea = new EditActions(fromSolution, toSolution, buildMethodActions(fromSolution, toSolution));

        var start = System.currentTimeMillis();

        /* ObjectOutputStream objectOutputStream = new ObjectOutputStream(
            new FileOutputStream(outputFile.toString()));
        objectOutputStream.writeObject(ea);
        objectOutputStream.close();
        */

        File actionsFile = new File(outputFile.toString());
        BufferedWriter writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
        for (Action action : ea.getEditActions()) {
            writer.write(ea.getActionName(action) + "\n");
        }
        writer.close();


        System.out.println("Saving actions took " + ((System.currentTimeMillis() - start) / 1000.0) + " s");
    }

    public static void clusterFolder(Path sourceFolder, Path storage,
                                     ClusteringAlgorithm algorithm,
                                     double distanceLimit, int minClustersCount) throws IOException {
        var baseTime = System.currentTimeMillis();
        var paths = Files.walk(sourceFolder);

        var changes = paths
                .filter(Files::isRegularFile)
                .map(x -> {
                    try {
                        var objectInputStream = new ObjectInputStream(
                                new FileInputStream(x.toString()));
                        Object result = objectInputStream.readObject();
                        objectInputStream.close();
                        return result instanceof Changes ? (Changes) result : null;
                    } catch (IOException | ClassNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println(getDiff(baseTime) + ": Changes folder processed, " + changes.size() + " edit scripts are loaded");
        doClustering(storage, baseTime, changes, algorithm, distanceLimit, minClustersCount);
    }

    public static void cluster(Path data, Path storage,
                               ClusteringAlgorithm algorithm,
                               double distanceLimit, int minClustersCount,
                               boolean parallel, boolean rename) throws IOException {
        var baseTime = System.currentTimeMillis();
        final Dataset dataset = ProtobufSerializationUtils.loadDataset(data);

        final var solutionGroups = dataset.getValues().stream().collect(Collectors.groupingBy(Solution::getId));

        List<Pair<Solution, Solution>> solutions = new ArrayList<>();

        for (var solutionGroup : solutionGroups.values()) {

            Solution rightOne =
                    solutionGroup
                            .stream()
                            .filter(x -> x.getVerdict() == OK)
                            .findFirst()
                            .orElseThrow(RuntimeException::new);

            solutions.addAll(
                    solutionGroup
                            .stream()
                            .filter(x -> x.getVerdict() == FAIL)
                            .map(x -> new Pair<Solution, Solution>(x, rightOne))
                            .collect(Collectors.toList()));
        }


        System.out.println(getDiff(baseTime) + ": Dataset and code files are loaded, " + solutions.size() + " tasks are formed");

        AtomicInteger completed = new AtomicInteger(0);

        final var solutionsStream = parallel ? solutions.parallelStream() : solutions.stream();

        List<Changes> changes =
                solutionsStream
                        .map(x -> {
                            final Changes change = getChanges(rename, x.first, x.second);

                            var localCompleted = completed.incrementAndGet();
                            if (localCompleted % 100 == 0) {
                                var threadDiff = getDiff(baseTime);
                                System.out.println(threadDiff + ": " + localCompleted + " tasks are done");
                            }

                            return change;
                        })
                        .collect(Collectors.toList());

        System.out.println(getDiff(baseTime) + ": All changes are processed, starting clustering");

        doClustering(storage, baseTime, changes, algorithm, distanceLimit, minClustersCount);
    }

    private static void doClustering(Path storage, long baseTime, List<Changes> changes,
                                     ClusteringAlgorithm algorithm,
                                     double distanceLimit, int minClustersCount) throws IOException {

        Clusterer<Changes> clusterer = algorithm.getClusterer(changes, distanceLimit, minClustersCount);

        final var clusters = clusterer.buildClusters(changes);

        System.out.println(getDiff(baseTime) + ": Clusters are formed, saving results");
        saveClustersToReadableFormat(clusters, storage);
        System.out.println(getDiff(baseTime) + ": Finished");
    }


    public static String[] splitPath(String pathString) {
        Path path = Paths.get(pathString);
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString)
                            .toArray(String[]::new);
    }

    private static void prepareESDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList, String version, int NgramSize) throws IOException {

        
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



    private static void clusterESDataset(Path pathToPrepared, Path pathToSaveCluster, Path pathToBugList, String version,  ClusteringAlgorithm algorithm,
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

  


  private static void prepareLASEDataset(Path pathToDataset, Path pathToSaveRepresentations, Path pathToBugList, String version) throws IOException {

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


    private static void MakeMaxTree(Path pathToDataset, Path pathToSaveRepresentations, String DefectA, String DefectB, String version) throws IOException {

        int processed=0;
        int skipped=0;

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
            TreeContext srcB= null;
            TreeContext dstA = null;
            TreeContext dstB = null ;
            Matcher matcherA = null;
            Matcher matcherB = null ;
            List<Action> actA = null ;
            List<Action> actB = null;



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
                        //double rate = ((double) fromFile.length()) / ((double) toFile.length());
                        System.out.println(getDiff(baseTime) + ": Checking size");
                        if(fromFile.length() <= MAX_FILE_SIZE && toFile.length() <= MAX_FILE_SIZE ){

                                // System.out.println(getDiff(baseTime) + ": Rate: " + rate ); //+" Files before: " + methodBeforePath.toString() +", after: " + methodAfterPath.toString());
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

                                    TreeContext src;
                                    TreeContext dst;

                                    ASTGenerator generator = null;

                                    if (version.toLowerCase().equals("abstract")) {
                                        generator = new CachedASTGenerator(  new NamesASTNormalizer() );
                                    }else{
                                        generator = new CachedASTGenerator(  new BasicASTNormalizer() );
                                    }

                                    src = generator.buildTreeContext(fromSolution);
                                    System.out.println("SRC tree size=" + src.getRoot().getSize());

                                    dst = generator.buildTreeContext(toSolution);
                                    System.out.println("DST tree size=" + dst.getRoot().getSize());
                                    
                                    TreeIoUtils.toXml(src ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\src_" + defectId );
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

                                    if(isB){
                                        srcB = src;
                                        dstB = dst;
                                        matcherB =matcherAst;
                                        actB = actions;
                                    }else{
                                        srcA = src;
                                        dstA = dst;
                                        matcherA =matcherAst;
                                        actA = actions;
                                        isB = true;

                                    }


                                    fromSolution = null;
                                    toSolution = null;

                                    if(actions != null  && actions.size() > 0 ){
                                        System.out.println(getDiff(baseTime) + ": Prepare es");
                                        

                                        emuCode = "";

                                        // store Actions
                                            int idx=0;
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
                                                
                                               /* 
                                                TreeContext tmp = new TreeContext();
                                                tmp.importTypeLabels(src);

                                               
                                                ITree node2add = action.getNode();
                                                List<ITree> lt = new ArrayList();

                                                while(! ActionContext.IsContextRoot(node2add)){
                                                    lt.add(node2add);
                                                    node2add =node2add.getParent();
                                                }

                                                if(lt.size()>0){
                                                    int i=lt.size()-1;
                                                    ITree root = tmp.createTree(
                                                        lt.get(i).getType()
                                                    ,lt.get(i).getLabel()
                                                    ,src.getTypeLabel(lt.get(i))
                                                    );
                                                    ITree prev =root;
                                                    ITree child =null;
                                                    while(i>0){
                                                        i--;
                                                        child = tmp.createTree(
                                                            lt.get(i).getType()
                                                        ,lt.get(i).getLabel()
                                                        ,src.getTypeLabel(lt.get(i))
                                                        );
                                                        prev.addChild(child);
                                                        prev = child;
                                                    }

                                                    tmp.setRoot( root );

                                                    idx++;
                                                    TreeIoUtils.toXml(tmp).writeTo(pathToSaveRepresentations.toString()+"\\ast\\" + defectId +"." + idx);
                                                }
                                                */
                                                
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
                                System.out.println(getDiff(baseTime) + ": Skip Defect id: " +  defectId +" Very large file  size." ); // Files before: " + methodBeforePath.toString() +", after: " + methodAfterPath.toString());
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

            System.out.println(getDiff(baseTime) + ": Defects are prepared. Try to build maxtree");

            if(srcA != null && dstB != null){

            testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(),new MappingStore());
                                   
            try {
                matcher.match();
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
                
            }

            ITree minSrc = matcher.GetLongestSrcSubtree();
            TreeContext mSrc = new TreeContext();
            mSrc.importTypeLabels(dstB);
            mSrc.setRoot(minSrc);
            mSrc.getRoot().refresh();

            try {
                TreeIoUtils.toXml(mSrc ).writeTo(pathToSaveRepresentations.toString()+"\\ast\\maxTree.xml" );
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
            }
            

            //ITree minDst = matcher.GetLongestDstSubtree();

           }

           

            
        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

    }


    private static void buildLASEbyCluster(Path pathToLaseDataset, Path pathToClusterFile,  Path pathToCommonActions, String version) throws IOException {
        
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

    private static List<String> BuildCommonActions(List<String> A, List<String> B){
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




    private static void saveClustersToReadableFormat(Clusters<Changes> clusters, Path storage) throws IOException {
        Clusters<String> idClusters = clusters.map(x -> x.getOrigin().getId());

        FileOutputStream fileStream = new FileOutputStream(storage.toFile(), false);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileStream));

        for (var cluster : idClusters.getClusters()) {
            bw.write(String.join(" ", cluster.getElements()));
            bw.newLine();
        }

        bw.close();
    }

    public static void mark(Path data, Path dst, int numExamples, int numClusters) throws IOException {
        final var clusters = ProtobufSerializationUtils.loadChangesClusters(data)
                .getClusters().stream()
                .sorted(Comparator.<Cluster<Changes>>comparingInt(Cluster::size).reversed())
                .collect(Collectors.toList());
        final var marks = new HashMap<Cluster<Changes>, String>();
        try (Scanner scanner = new Scanner(System.in)) {
            for (var cluster : clusters.subList(0, Math.min(clusters.size(), numClusters))) {
                System.out.println("Next cluster (size=" + cluster.size() + "):");
                final var solutions = cluster.elementsCopy();
                Collections.shuffle(solutions);
                for (int i = 0; i < Math.min(numExamples, solutions.size()); i++) {
                    final var solution = solutions.get(i);
                    System.out.println("    Example #" + i);
                    System.out.println("    Id: " + solution.getOrigin().getId());
                    System.out.println(solution.getOrigin().getCode());
                    System.out.println();
                    System.out.println("    Submission fix:");
                    solution.getChanges().forEach(System.out::println);
                    System.out.println();
                }
                System.out.println("-------------------------------------------------");
                System.out.println("Your mark:");
                while (true) {
                    final String mark = scanner.nextLine();
                    if (mark.equals("-")) {
                        marks.remove(cluster);
                    } else if (mark.equals("+")) {
                        System.out.println("Final mark: " + marks.get(cluster));
                        break;
                    } else {
                        marks.put(cluster, mark);
                    }
                }
            }
        }
        final var marked = new MarkedClusters<>(marks);
        ProtobufSerializationUtils.storeMarkedChangesClusters(marked, dst);
    }

    public static void prepare(Path marks, Path data, Path dst) throws IOException {
        final Dataset dataset = ProtobufSerializationUtils.loadDataset(data);
        final ASTGenerator astGenerator = new CachedASTGenerator(new NamesASTNormalizer());
        final ChangeGenerator changeGenerator = new BasicChangeGenerator(
                astGenerator,
                Collections.singletonList((x, y) -> new CompositeMatchers.ClassicGumtree(x, y, new MappingStore())));
        final Unifier<Solution> unifier = new BasicUnifier<>(
                CommonUtils.compose(astGenerator::buildTree, ITree::getHash)::apply,
                CommonUtils.checkEquals(astGenerator::buildTree, ASTUtils::deepEquals),
                new MinValuePicker<>(Comparator.comparing(Solution::getSolutionId)));
        final OptionSelector<Solution, Solution> selector = new ClosestPairSelector<>(
                unifier.unify(dataset.getValues(CommonUtils.check(Solution::getVerdict, OK::equals))),
                new HeuristicChangesBasedDistanceFunction(changeGenerator));
        final var extractor = new CachedFeaturesExtractor<>(
                new ChangesExtractor(changeGenerator, selector),
                Solution::getSolutionId);
        final var changes = ProtobufSerializationUtils.loadMarkedChangesClusters(marks);
        final var prepared = changes.map(change -> extractor.process(change.getOrigin()));
        ProtobufSerializationUtils.storeMarkedChangesClusters(prepared, dst);
    }

    public static void classify(Path data, Path marks, Path element) throws IOException {
//        final MarkedClusters<Solution, String> clusters = ProtobufSerializationUtils.loadMarkedClusters(marks);
//        final var dataset = ProtobufSerializationUtils.loadDataset(data);
//        final ASTGenerator astGenerator = new CachedASTGenerator(new NamesASTNormalizer());
//        final ChangeGenerator changeGenerator = new BasicChangeGenerator(astGenerator);
//        final Unifier<Solution> unifier = new BasicUnifier<>(
//                CommonUtils.compose(astGenerator::buildTree, ITree::getHash)::apply,
//                CommonUtils.checkEquals(astGenerator::buildTree, ASTUtils::deepEquals),
//                new MinValuePicker<>(Comparator.comparingInt(Solution::getSolutionId)));
//        final DistanceFunction<Solution> metric =
//                new HeuristicChangesBasedDistanceFunction(changeGenerator);
//        final OptionSelector<Solution, Solution> selector = new ClosestPairSelector<>(
//                unifier.unify(dataset.getValues(x -> x.getVerdict() == OK)), metric);
//        final var extractor = new ChangesExtractor(changeGenerator, selector);
//        final var approach = FuzzyJaccardApproach.getDefaultApproach(extractor);
//        final var distanceFunction = CommonUtils.metricFor(approach.metric, Wrapper<Changes, Solution>::getFeatures);
//        final Classifier<Solution, String> classifier = new CompositeClassifier<>(
//                approach.extractor,
//                new KNearestNeighbors<>(15, distanceFunction));
//        classifier.train(clusters);
//        final var code = new String(Files.readAllBytes(element));
//        final var solution = new Solution(code, -1, -1, -1, FAIL);
//        final var result = classifier.mostProbable(solution);
//        System.out.println("Solution:");
//        System.out.println(code);
//        System.out.println("Result: " + result.getKey() + " (" + result.getValue() + ")");
        throw new UnsupportedOperationException();
    }

    private static double getDiff(long baseTime) {
        return (System.currentTimeMillis() - baseTime) / 1000.0;
    }

    private static Changes getChanges(boolean rename, Solution fromSolution, Solution toSolution) {
        final ASTGenerator astGenerator =
                new CachedASTGenerator(
                        rename ? null : new NamesASTNormalizer()
                );
        final ChangeGenerator changeGenerator = new BasicChangeGenerator(astGenerator);
        return changeGenerator.getChanges(fromSolution, toSolution);
    }
}
