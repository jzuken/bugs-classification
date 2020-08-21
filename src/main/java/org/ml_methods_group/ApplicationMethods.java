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

public class ApplicationMethods {

    static final int MAX_FILE_SIZE = 200 * 1024;

    // COMMON protected functions
    protected static ITree buildTree(Solution s) {
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

    protected static List<Action> buildMethodActions(Solution FileBefore, Solution FileAfter)
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
  
    protected static void doClustering(Path storage, long baseTime, List<Changes> changes,
                                     ClusteringAlgorithm algorithm,
                                     double distanceLimit, int minClustersCount) throws IOException {

        Clusterer<Changes> clusterer = algorithm.getClusterer(changes, distanceLimit, minClustersCount);

        final var clusters = clusterer.buildClusters(changes);

        System.out.println(getDiff(baseTime) + ": Clusters are formed, saving results");
        saveClustersToReadableFormat(clusters, storage);
        System.out.println(getDiff(baseTime) + ": Finished");
    }

    protected static String[] splitPath(String pathString) {
        Path path = Paths.get(pathString);
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString)
                            .toArray(String[]::new);
    }
   
    protected static double getDiff(long baseTime) {
        return (System.currentTimeMillis() - baseTime) / 1000.0;
    }

    protected static Changes getChanges(boolean rename, Solution fromSolution, Solution toSolution) {
        final ASTGenerator astGenerator =
                new CachedASTGenerator(
                        rename ? null : new NamesASTNormalizer()
                );
        final ChangeGenerator changeGenerator = new BasicChangeGenerator(astGenerator);
        return changeGenerator.getChanges(fromSolution, toSolution);
    }
 
    protected static void saveClustersToReadableFormat(Clusters<Changes> clusters, Path storage) throws IOException {
        Clusters<String> idClusters = clusters.map(x -> x.getOrigin().getId());

        FileOutputStream fileStream = new FileOutputStream(storage.toFile(), false);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileStream));

        for (var cluster : idClusters.getClusters()) {
            bw.write(String.join(" ", cluster.getElements()));
            bw.newLine();
        }

        bw.close();
    }


    // public methods
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

    public static void jaccardTreeWeight(Path pathToMaxTreeDir, String DefectA, String DefectB, String version) throws IOException {

        try {
//            get all the files in MaxTree directory
            List<String> result = Files.walk(Paths.get(pathToMaxTreeDir.toString())).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            String defectAPath = "";
            String defectBPath = "";
            String maxTreePath = "";

            for (String fName : result) {
//                System.out.println(fName);
//                C:\Users\kWX910209\Documents\gumtree_csv\maxtree\ast\dst_DTS2016121309461_conctrete_link_egress.c
//                C:\Users\kWX910209\Documents\gumtree_csv\maxtree\ast\maxTree_DTS2016120602239_to_DTS2016121309461.xml
//                C:\Users\kWX910209\Documents\gumtree_csv\maxtree\ast\src_DTS2016120602239_conctrete_tnl-neigh-cache.c
//                C:\Users\kWX910209\Documents\gumtree_csv\maxtree\ast\src_DTS2016121309461_conctrete_link_egress.c

//                get filenames
                String[] pathParts = fName.split("\\\\");
                String filename = pathParts[pathParts.length - 1];
                filename = filename.substring(0, filename.lastIndexOf('.'));
                String[] filenameParts = filename.split("_");


                if (filenameParts[0].equals("src")) {
                    if (filenameParts[1].equals(DefectA)) {
                        defectAPath = fName;
                    } else if (filenameParts[1].equals(DefectB)) {
                        defectBPath = fName;
                    }

                }
//              should they be reversable?
                if (filenameParts[0].equals("maxTree") && filenameParts[1].equals(DefectA) && filenameParts[3].equals(DefectB)) {
                    maxTreePath = fName;
                }
            }


//            Code with sets

            Set<String> maxTreeSet = new HashSet();
            Set<String> defectASet = new HashSet();
            Set<String> defectBSet = new HashSet();

            if (!defectAPath.equals("") && !defectBPath.equals("")) {
                Scanner defectAInput = new Scanner(new File(defectAPath));
                while (defectAInput.hasNextLine())
                {
                    defectASet.add(defectAInput.nextLine());
                }
                Scanner defectBInput = new Scanner(new File(defectBPath));
                while (defectBInput.hasNextLine())
                {
                    defectBSet.add(defectBInput.nextLine());
                }
            }

            if (!maxTreePath.equals("")) {
                Scanner maxTreeInput = new Scanner(new File(maxTreePath));
                while (maxTreeInput.hasNextLine())
                {
                    maxTreeSet.add(maxTreeInput.nextLine());
                }

            }

            if (!maxTreeSet.isEmpty() && !defectASet.isEmpty() && !defectBSet.isEmpty()) {


                Set<String> unionSet = new HashSet<>(defectASet);
                unionSet.addAll(defectBSet);

                int c = maxTreeSet.size() * 2;
                int ab = unionSet.size();

                System.out.println("---");
                System.out.println((float) c / ab);
            }



        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }


    }
}