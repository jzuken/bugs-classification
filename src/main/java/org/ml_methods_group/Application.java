package org.ml_methods_group;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.clustering.clusterers.CompositeClusterer;
import org.ml_methods_group.clustering.clusterers.HAC;
import org.ml_methods_group.common.*;
import org.ml_methods_group.common.ast.ASTUtils;
import org.ml_methods_group.common.ast.changes.BasicChangeGenerator;
import org.ml_methods_group.common.ast.changes.ChangeGenerator;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.common.ast.editactions.EditActions;
import org.ml_methods_group.common.ast.editactions.EditActionStore;
import org.ml_methods_group.common.ast.changes.CodeChange;
import org.ml_methods_group.common.ast.generation.ASTGenerator;
import org.ml_methods_group.common.ast.generation.CachedASTGenerator;
import org.ml_methods_group.common.ast.normalization.NamesASTNormalizer;
import org.ml_methods_group.common.extractors.BOWExtractor;
import org.ml_methods_group.common.extractors.BOWExtractor.BOWVector;
import org.ml_methods_group.common.extractors.ChangesExtractor;
import org.ml_methods_group.common.extractors.HashExtractor;
import org.ml_methods_group.common.metrics.functions.HeuristicChangesBasedDistanceFunction;
import org.ml_methods_group.common.metrics.selectors.ClosestPairSelector;
import org.ml_methods_group.common.preparation.Unifier;
import org.ml_methods_group.common.preparation.basic.BasicUnifier;
import org.ml_methods_group.common.preparation.basic.MinValuePicker;
import org.ml_methods_group.common.serialization.ProtobufSerializationUtils;
import org.ml_methods_group.parsing.ParsingUtils;
import org.ml_methods_group.testing.extractors.CachedFeaturesExtractor;
import org.ml_methods_group.common.ast.generation.BasicASTGenerator;
import org.ml_methods_group.common.ast.normalization.BasicASTNormalizer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;
import static org.ml_methods_group.evaluation.approaches.BOWApproach.*;

public class Application {

    static final double DEFAULT_DISTANCE_LIMIT = 0.3;
    static final int DEFAULT_MIN_CLUSTERS_COUNT = 1;

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
                if (args.length < 4 || args.length > 5 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Problem id" + System.lineSeparator() +
                            "    Path to code file before changes" + System.lineSeparator() +
                            "    Path to code file after changes" + System.lineSeparator() +
                            "    Path to file to store edit script" + System.lineSeparator() );
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
                if (args.length < 3 || args.length > 3 ) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to code dataset" + System.lineSeparator() +
                            "    Path to store representation" + System.lineSeparator() );
                    return;
                }
                prepareESDataset (
                        Paths.get(args[1]),
                        Paths.get(args[2])
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
                if (args.length < 3 || args.length > 5) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to folder with edit scripts" + System.lineSeparator() +
                            "    Path to file to store clusters" + System.lineSeparator() +
                            "    [Optional] --distanceLimit=X - Set clustering distance limit to X, default value 0.3" + System.lineSeparator() +
                            "    [Optional] --minClustersCount=X - Set minimum amount of clusters to X, default value 1" + System.lineSeparator()
                    );
                    return;
                }
                clusterFolder(
                        Paths.get(args[1]),
                        Paths.get(args[2]),
                        getDistanceLimitFromArgs(args),
                        getMinClustersCountFromArgs(args));
                break;
            case "cluster":
                if (args.length < 3 || args.length > 7) {
                    System.out.println("Wrong number of arguments! Expected:" + System.lineSeparator() +
                            "    Path to file which store parsed solutions" + System.lineSeparator() +
                            "    Path to file to store clusters" + System.lineSeparator() +
                            "    [Optional] --parallel to execute changes generation in parallel, default - false" + System.lineSeparator() +
                            "    [Optional] --rename to rename variable names to generic names, default - false" + System.lineSeparator() +
                            "    [Optional] --distanceLimit=X - Set clustering distance limit to X, default value 0.3" + System.lineSeparator() +
                            "    [Optional] --minClustersCount=X - Set minimum amount of clusters to X, default value 1" + System.lineSeparator());
                    return;
                }

                cluster(Paths.get(args[1]), Paths.get(args[2]),
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
        var param = Arrays.stream(args).filter(x->x.toLowerCase().startsWith("--distancelimit")).findFirst();

        if(param.isEmpty()){
            return DEFAULT_DISTANCE_LIMIT;
        }

        return Double.parseDouble(param.get().toLowerCase().replace("--distancelimit=", ""));
    }

    private static int getMinClustersCountFromArgs(String[] args) {
        var param = Arrays.stream(args).filter(x->x.toLowerCase().startsWith("--minclusterscount")).findFirst();

        if(param.isEmpty()){
            return DEFAULT_MIN_CLUSTERS_COUNT;
        }

        return Integer.parseInt(param.get().toLowerCase().replace("--minclusterscount=", ""));
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
        generator.generate();

        return generator.getActions();
    }

    public static void makeEditScript(String id, Path fromFile, Path toFile, Path outputFile) throws IOException {
        final var fromCode = Files.readString(fromFile);
        final String wrongSolutionId = id + FAIL.ordinal();
        final var fromSolution = new Solution(fromCode, id, wrongSolutionId, FAIL);

        final var toCode = Files.readString(toFile);
        final String rightSolutionId = id + OK.ordinal();
        final var toSolution = new Solution(toCode, id, rightSolutionId, OK);

        final EditActions ea = new EditActions(fromSolution,toSolution,buildMethodActions(fromSolution,toSolution));

        var start = System.currentTimeMillis();
        
        /* ObjectOutputStream objectOutputStream = new ObjectOutputStream(
            new FileOutputStream(outputFile.toString()));

        objectOutputStream.writeObject(ea);
        objectOutputStream.close();
        */

        File actionsFile = new File(outputFile.toString());
        BufferedWriter writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
        for (Action action : ea.getEditActions()) {
                writer.write(ea.getActionName(action)+ "\n");
        }
        writer.close();
        

        System.out.println("Saving actions took " + ((System.currentTimeMillis() - start) / 1000.0) + " s");
    }


    public static void clusterFolder(Path sourceFolder, Path storage, double distanceLimit, int minClustersCount) throws IOException {
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
        doClustering(storage, baseTime, changes, distanceLimit, minClustersCount);
    }

    public static void cluster(Path data, Path storage, double distanceLimit, int minClustersCount, boolean parallel, boolean rename) throws IOException {
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

        doClustering(storage, baseTime, changes, distanceLimit, minClustersCount);
    }

    private static void doClustering(Path storage, long baseTime, List<Changes> changes,
                                     double distanceLimit, int minClustersCount) throws IOException {
        final var bowExtractor = getBOWExtractor(20000, changes);

        final Clusterer<Changes> clusterer = new CompositeClusterer<>(bowExtractor, new HAC<>(
                distanceLimit,
                minClustersCount,
                CommonUtils.metricFor(BOWExtractor::cosineDistance, Wrapper::getFeatures)));
        final var clusters = clusterer.buildClusters(changes);

        System.out.println(getDiff(baseTime) + ": Clusters are formed, saving results");
        saveClustersToReadableFormat(clusters, storage);
        System.out.println(getDiff(baseTime) + ": Finished");
    }


    private static void prepareESDataset(Path pathToDataset,  Path pathToSaveRepresentations) throws IOException {


            EditActionStore store = new EditActionStore();

            File datasetDir = new File(pathToDataset.toString());
            File[] elements = Arrays.stream(datasetDir.listFiles())
            .toArray(File[]::new);

            // Arrays.sort(elements, Comparator.comparingString(o -> o.getName()));
            Integer id= 1;
            for (File elementDir : elements) {
           
                Path methodBeforePath = pathToDataset.resolve(elementDir.getName()).resolve("before.txt");
                Path methodAfterPath = pathToDataset.resolve(elementDir.getName()).resolve("after.txt");
                var fromCode = Files.readString(methodBeforePath);
                String wrongSolutionId = Integer.toString(id) + FAIL.ordinal();
                var fromSolution = new Solution(fromCode, Integer.toString(id), wrongSolutionId, FAIL);

                var toCode = Files.readString(methodAfterPath);
                String rightSolutionId = Integer.toString(id) + OK.ordinal();
                var toSolution = new Solution(toCode, Integer.toString(id), rightSolutionId, OK);
                List<Action> actions = buildMethodActions(fromSolution, toSolution);
                Pair<List<String>, List<String>> actionsStrings = store.convertToStrings(actions);

                store.addActions(elementDir.getName(), actionsStrings.getSecond());
                id++;
            
            }
            System.out.println("Saving representation");
            store.saveRepresentationsBitset(pathToSaveRepresentations.toString(), null);
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

    public static FeaturesExtractor<Changes, BOWVector> getBOWExtractor(int wordsLimit, List<Changes> data) {
        final var weak = HashExtractor.<CodeChange.NodeContext>builder()
                .append("TOC")
                .hashComponent(CodeChange.NodeContext::getNode, TYPE_ONLY_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, TYPE_ONLY_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, TYPE_ONLY_NODE_STATE_HASH)
                .build();
        final var javaTypes = HashExtractor.<CodeChange.NodeContext>builder()
                .append("JTC")
                .hashComponent(CodeChange.NodeContext::getNode, JAVA_TYPE_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, TYPE_ONLY_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, TYPE_ONLY_NODE_STATE_HASH)
                .build();
        final var full = HashExtractor.<CodeChange.NodeContext>builder()
                .append("FCC")
                .hashComponent(CodeChange.NodeContext::getNode, FULL_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, TYPE_ONLY_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, TYPE_ONLY_NODE_STATE_HASH)
                .build();
        final var extended = HashExtractor.<CodeChange.NodeContext>builder()
                .append("ECC")
                .hashComponent(CodeChange.NodeContext::getNode, JAVA_TYPE_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, LABEL_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, TYPE_ONLY_NODE_STATE_HASH)
                .build();
        final var fullExtended = HashExtractor.<CodeChange.NodeContext>builder()
                .append("FEC")
                .hashComponent(CodeChange.NodeContext::getNode, FULL_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, LABEL_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, TYPE_ONLY_NODE_STATE_HASH)
                .build();
        final var deepExtended = HashExtractor.<CodeChange.NodeContext>builder()
                .append("DEC")
                .hashComponent(CodeChange.NodeContext::getNode, JAVA_TYPE_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParent, LABEL_NODE_STATE_HASH)
                .hashComponent(CodeChange.NodeContext::getParentOfParent, LABEL_NODE_STATE_HASH)
                .build();
        final var codeChanges = data.stream()
                .map(Changes::getChanges)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        final var extractors = Arrays.asList(getCodeChangeHasher(weak),
                getCodeChangeHasher(javaTypes), getCodeChangeHasher(full), getCodeChangeHasher(extended),
                getCodeChangeHasher(fullExtended), getCodeChangeHasher(deepExtended));
        final HashMap<String, Integer> dict = BOWExtractor.mostCommon(
                extractors,
                codeChanges,
                wordsLimit);
        return new BOWExtractor<>(dict, extractors).extend(Changes::getChanges);
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
