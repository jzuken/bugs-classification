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
import org.ml_methods_group.ApplicationMethods;
import org.ml_methods_group.ApplicationLASE;
import org.ml_methods_group.ApplicationSuggest;

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
                ApplicationMethods.createEditScript(
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
                ApplicationMethods.makeEditScript(
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
                ApplicationES.prepareESDataset(
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
                ApplicationES.clusterESDataset(
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
                ApplicationLASE.buildLASEbyCluster(
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
                ApplicationLASE.prepareLASEDataset(
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
                            "    Id of defect to test (bad only file)" + System.lineSeparator() +
                            "    Id of defect used as template (bad + good)" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationLASE.MakeMaxTree(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        args[3],
                        args[4],
                        args[5]
                );
                break;


                case "make.maxforest":
                if (args.length != 6 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code dataset" + System.lineSeparator() +
                            "    Path to store representation" + System.lineSeparator() +
                            "    Id of defect to test (bad only file)" + System.lineSeparator() +
                            "    Id of defect used as template (bad + good)" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationLASE.MakeMaxForest(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        args[3],
                        args[4],
                        args[5]
                );
                break;

                case "matrix.maxtree":
                if (args.length != 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to lase dataset" + System.lineSeparator() +
                            "    Path to cluster list file" + System.lineSeparator() +
                            "    Path to store matrix" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationLASE.scanCluster(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4]
                );
                break;


                case "top10.maxtree":
                if (args.length < 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to dataset" + System.lineSeparator() +
                            "    Path to defect list file" + System.lineSeparator() +
                            "    Path to store matrix" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() +
                            "    [Optional] --verbose=yes  for detail output" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationLASE.getTop10(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        args[4],  
                        getVerboseFromArgs(args)
                );
                break;


                case "looklike":
                if (args.length != 7 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to dataset with test data" + System.lineSeparator() +
                            "    Path to file with list of defect to test" + System.lineSeparator() +
                            "    Path to dataset with template defects" + System.lineSeparator() +
                            "    Path to file with list of template defects" + System.lineSeparator() +
                            "    Path to folder to store matrix" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationLASE.lookLike(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        Paths.get(args[4]),
                        Paths.get(args[5]),
                        args[6]
                );
                break;

            case "lase.looklike":
                if (args.length < 7 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to dataset with test data" + System.lineSeparator() +
                            "    Path to file with list of defect to test" + System.lineSeparator() +
                            "    Path to dataset with template defects" + System.lineSeparator() +
                            "    Path to file with list of template defects" + System.lineSeparator() +
                            "    Path to folder to store matrix" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract)" + System.lineSeparator() +
                            "    [Optional] --verbose=yes  for detail output" + System.lineSeparator() +
                            "    [Optional] --markers=N  minimal markers quantity for similarity check, default 5" + System.lineSeparator() +
                            "    [Optional] --anynode=true|false  use any node for similarity check or only node with label, default false" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationSuggest.LaseLookLike(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        Paths.get(args[4]),
                        Paths.get(args[5]),
                        args[6],  
                        getVerboseFromArgs(args),
                        getNamedIntegerFromArgs("markers", 5, args),
                        getNamedBooleanFromArgs("anynode", false, args)
                        
                );
                break;


            case "suggestion":
                if (args.length < 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file for test" + System.lineSeparator() +
                            "    Path to bug library " + System.lineSeparator() +
                            "    Path to file with list of bug library defects" + System.lineSeparator() +
                            "    Path to folder to store suggestion" + System.lineSeparator() +
                            "    [Optional] --verbose=yes  for detail output" + System.lineSeparator() +
                            "    [Optional] --markers=N  minimal markers quantity for similarity check, default 5" + System.lineSeparator() +
                            "    [Optional] --similarity=S  similarity level for generate suggestion, default 90" + System.lineSeparator() +
                            "    [Optional] --anynode=true|false  use any node for similarity checr or only node with label, default false" + System.lineSeparator() 
                            );
                    return;
                }
                ApplicationSuggest.Suggestion(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        Paths.get(args[3]),
                        Paths.get(args[4]),
                        getVerboseFromArgs(args),
                        getNamedIntegerFromArgs("markers", 5, args),
                        getNamedIntegerFromArgs("similarity", 90, args),
                        getNamedBooleanFromArgs("anynode", false, args)
                        
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
                ApplicationMethods.clusterFolder(
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

                ApplicationMethods.cluster(Paths.get(args[1]), Paths.get(args[2]),
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
                ApplicationMethods.mark(Paths.get(args[1]), Paths.get(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                break;
            case "prepare":
                if (args.length != 4) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file which store marks" + System.lineSeparator() +
                            "    Path to file which store marks parsed solutions" + System.lineSeparator() +
                            "    Path to file to store prepared marked data" + System.lineSeparator());
                    return;
                }
                ApplicationMethods.prepare(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
                break;
            case "jaccardTreeWeight":
                if (args.length != 5) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to MaxTree folder" + System.lineSeparator() +
                            "    First Defect ID" + System.lineSeparator() +
                            "    Second Defect ID" + System.lineSeparator() +
                            "    LASE variant (conctrete,  abstract) " + System.lineSeparator());
                    return;
                }
                ApplicationMethods.jaccardTreeWeight(
                        Paths.get(args[1]),
                        args[2],
                        args[3],
                        args[4]
                );
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

    private static String getVerboseFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith("--verbose")).findFirst();

        if (param.isEmpty()) {
            return "no";
        }

        return param.get().toLowerCase().replace("--verbose=", "");
    }

    private static Integer getNamedIntegerFromArgs(String Name, Integer defaultValue, String[] args) {
        String pName = "--" +Name.toLowerCase()+"=";
        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith(pName)).findFirst();
        System.out.println(pName +"->"+param.get());
        if (param.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(param.get().toLowerCase().replace(pName, ""));
    }


    private static Boolean getNamedBooleanFromArgs(String Name, Boolean defaultValue, String[] args) {
        String pName = "--" +Name.toLowerCase()+"=";

        var param = Arrays.stream(args).filter(x -> x.toLowerCase().startsWith(pName)).findFirst();

        System.out.println(pName +"->"+param.get());
        
        if (param.isEmpty()) {
            return defaultValue;
        }
        
        System.out.println(param.get()  +"-->" + param.get().toLowerCase().replace(pName, ""));
        return param.get().toLowerCase().replace(pName, "").startsWith("true");
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

   
}
