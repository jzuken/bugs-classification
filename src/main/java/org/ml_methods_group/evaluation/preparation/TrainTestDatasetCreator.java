package org.ml_methods_group.evaluation.preparation;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.serialization.ProtobufSerializationUtils;
import org.ml_methods_group.evaluation.EvaluationInfo;
import org.ml_methods_group.parsing.JavaCodeValidator;
import org.ml_methods_group.parsing.ParsingUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;

public class TrainTestDatasetCreator {
    public static void main(String[] args) throws IOException {
        int testSetSize = 200;
        long seed = 124345;

        final Random random = new Random(seed);
        final Dataset dataset;
        try (InputStream fis = TrainTestDatasetCreator.class.getResourceAsStream("/dataset2.csv")) {
            dataset = ParsingUtils.parse(fis);
        }
        final var incorrectIds = new LinkedHashMap<String, Set<String>>();
        final var correctIds = new LinkedHashMap<String, Set<String>>();
        for (Solution solution : dataset.getValues()) {
            final var tmp = solution.getVerdict() == FAIL ? incorrectIds : correctIds;
            tmp.computeIfAbsent(solution.getId(), x -> new LinkedHashSet<>())
                    .add(solution.getId());
        }
        final var testSet = new HashSet<String>();
        for (var problemId : correctIds.keySet()) {
            final var tmp = correctIds.getOrDefault(problemId, Collections.emptySet());
            final var permutation = incorrectIds.get(problemId).stream()
                    .filter(tmp::contains)
                    .collect(Collectors.toList());
            Collections.shuffle(permutation, random);
            if (permutation.size() < testSetSize) {
                continue;
            }
            testSet.addAll(permutation.subList(0, testSetSize));
        }
        ProtobufSerializationUtils.storeDataset(
                dataset.filter(x -> testSet.contains(x.getId())),
                EvaluationInfo.PATH_TO_DATASET.resolve("test_dataset.tmp"));
        ProtobufSerializationUtils.storeDataset(
                dataset.filter(x -> !testSet.contains(x.getId())),
                EvaluationInfo.PATH_TO_DATASET.resolve("train_dataset.tmp"));
    }
}
