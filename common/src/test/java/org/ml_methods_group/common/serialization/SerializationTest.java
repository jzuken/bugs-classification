package org.ml_methods_group.common.serialization;

import org.junit.Test;
import org.ml_methods_group.common.*;
import org.ml_methods_group.common.ast.changes.ChangeType;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.common.ast.changes.CodeChange;
import org.ml_methods_group.common.ast.changes.CodeChange.NodeContext;
import org.ml_methods_group.common.ast.changes.CodeChange.NodeState;
import org.ml_methods_group.common.proto.*;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.deepEquals;
import static org.junit.Assert.*;
import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;
import static org.ml_methods_group.common.ast.NodeType.*;


public class SerializationTest {

    @FunctionalInterface
    private interface UnsafeBiConsumer<T, V> {
        void accept(T value1, V value2) throws Exception;
    }

    @FunctionalInterface
    private interface UnsafeFunction<T, V> {
        V apply(T value) throws Exception;
    }

    private static <T, V> T writeAndRead(T value,
                                         Function<T, V> mapper,
                                         Function<V, T> invertedMapper,
                                         UnsafeBiConsumer<V, OutputStream> writer,
                                         UnsafeFunction<InputStream, V> reader) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.accept(mapper.apply(value), out);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return invertedMapper.apply(reader.apply(in));
    }

    private static final CodeChange CODE_CHANGE_EXAMPLE;

    static {
        final NodeState nodeBefore = new NodeState(MY_VARIABLE_NAME, "String", "String@1", "token", 0);
        final NodeState parentBefore = new NodeState(MY_METHOD_INVOCATION_ARGUMENTS, null, "parse", null, 1);
        final NodeState parentOfParentBefore = new NodeState(METHOD_INVOCATION, null, "parse", null, 0);
        final NodeState[] unclesBefore = {
                new NodeState(SIMPLE_NAME, null, "parse", null, 0),
                parentBefore
        };
        final NodeState[] brothersBefore = {
                nodeBefore,
                new NodeState(NUMBER_LITERAL, null, "10", null, 1),
                new NodeState(NULL_LITERAL, null, "", null, 2)
        };
        final NodeState[] childrenBefore = {};
        final NodeContext contextBefore = new NodeContext(nodeBefore, parentBefore, parentOfParentBefore,
                unclesBefore, brothersBefore, childrenBefore);

        final NodeState nodeAfter = new NodeState(MY_VARIABLE_NAME, "String", "String@2", "text", 0);
        final NodeState parentAfter = new NodeState(MY_METHOD_INVOCATION_ARGUMENTS, null, "parse", null, 1);
        final NodeState parentOfParentAfter = new NodeState(METHOD_INVOCATION, null, "parse", null, 0);
        final NodeState[] unclesAfter = {
                new NodeState(SIMPLE_NAME, null, "parse", null, 0),
                parentAfter
        };
        final NodeState[] brothersAfter = {
                nodeAfter,
                new NodeState(NUMBER_LITERAL, null, "100", null, 1),
                new NodeState(NULL_LITERAL, null, "", null, 2)
        };
        final NodeState[] childrenAfter = {};
        final NodeContext contextAfter = new NodeContext(nodeAfter, parentAfter, parentOfParentAfter,
                unclesAfter, brothersAfter, childrenAfter);
        CODE_CHANGE_EXAMPLE = new CodeChange(contextBefore, contextAfter, ChangeType.UPDATE);
    }

    @Test
    public void testSolutionTransformation() throws Exception {
        final Solution solution = new Solution("some code", "2", "3", OK);
        final Solution parsedSolution = writeAndRead(solution,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoSolution::writeTo,
                ProtoSolution::parseFrom);
        assertEquals(solution, parsedSolution);
    }

    @Test
    public void testNodeStateTransformation() throws Exception {
        final NodeState state = new NodeState(MY_VARIABLE_NAME, "int", "int@1",
                "cnt", 0);
        final NodeState parsedState = writeAndRead(state,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoNodeState::writeTo,
                ProtoNodeState::parseFrom);
        assertEquals(state, parsedState);
    }

    @Test
    public void testNodeContextTransformation() throws Exception {
        final NodeContext context = CODE_CHANGE_EXAMPLE.getOriginalContext();
        final NodeContext parsedContext = writeAndRead(context,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoNodeContext::writeTo,
                ProtoNodeContext::parseFrom);
        assertEquals(context, parsedContext);
    }

    @Test
    public void testCodeChangeTransformation() throws Exception {
        final CodeChange codeChange = CODE_CHANGE_EXAMPLE;
        final CodeChange parsedChange = writeAndRead(codeChange,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoAtomicChange::writeTo,
                ProtoAtomicChange::parseFrom);
        assertEquals(codeChange, parsedChange);
    }

    @Test
    public void testChangesTransformation() throws Exception {
        final Changes changes = new Changes(
                new Solution("code before", "1", "1", FAIL),
                new Solution("code after", "1", "2", OK),
                Collections.singletonList(CODE_CHANGE_EXAMPLE));
        final Changes parsedChanges = writeAndRead(changes,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoChanges::writeTo,
                ProtoChanges::parseFrom);
        assertEquals(changes, parsedChanges);
        assertArrayEquals(changes.getChanges().toArray(), parsedChanges.getChanges().toArray());
    }

    @Test
    public void testMarkedChangesClusters() throws Exception {
        final Solution before = new Solution("some code", "1", "1", FAIL);
        final Solution after = new Solution("some another code", "1", "2", OK);
        final Changes changes = new Changes(before, after, Collections.singletonList(CODE_CHANGE_EXAMPLE));
        final Cluster<Changes> cluster = new Cluster<>(Collections.singletonList(changes));
        final MarkedClusters<Changes, String> data = new MarkedClusters<>(Collections.singletonMap(cluster, "mark"));
        final MarkedClusters<Changes, String> parsedData =
                SerializationTest.writeAndRead(data,
                        EntityToProtoUtils::transformMarkedChangesClusters,
                        ProtoToEntityUtils::transform,
                        ProtoMarkedChangesClusters::writeTo,
                        ProtoMarkedChangesClusters::parseFrom);
        assertEquals(data.getFlatMarks().size(), parsedData.getFlatMarks().size());
        assertEquals(data.getFlatMarks().values().iterator().next(),
                parsedData.getFlatMarks().values().iterator().next());
        assertEquals(data.getFlatMarks().keySet().iterator().next().getChanges().size(),
                parsedData.getFlatMarks().keySet().iterator().next().getChanges().size());
        assertEquals(data.getFlatMarks().keySet().iterator().next().getChanges().get(0),
                parsedData.getFlatMarks().keySet().iterator().next().getChanges().get(0));
        assertEquals(data.getFlatMarks().keySet().iterator().next().getOrigin(),
                parsedData.getFlatMarks().keySet().iterator().next().getOrigin());
        assertEquals(data.getFlatMarks().keySet().iterator().next().getTarget(),
                parsedData.getFlatMarks().keySet().iterator().next().getTarget());
    }

    @Test
    public void testChangesClusters() throws Exception {
        final Solution before = new Solution("some code", "1", "1", FAIL);
        final Solution after = new Solution("some another code", "1", "2", OK);
        final Changes changes = new Changes(before, after, Collections.singletonList(CODE_CHANGE_EXAMPLE));
        final Cluster<Changes> cluster = new Cluster<>(Collections.singletonList(changes));
        final Clusters<Changes> data = new Clusters<>(Collections.singletonList(cluster));
        final Clusters<Changes> parsedData = writeAndRead(data,
                EntityToProtoUtils::transformChangesClusters,
                ProtoToEntityUtils::transform,
                ProtoChangesClusters::writeTo,
                ProtoChangesClusters::parseFrom);
        assertEquals(data.getClusters().size(), parsedData.getClusters().size());
        assertEquals(data.getClusters().iterator().next().size(),
                parsedData.getClusters().iterator().next().size());
        assertEquals(data.getClusters().iterator().next().getElements().get(0).getOrigin(),
                parsedData.getClusters().iterator().next().getElements().get(0).getOrigin());
        assertEquals(data.getClusters().iterator().next().getElements().get(0).getTarget(),
                parsedData.getClusters().iterator().next().getElements().get(0).getTarget());
        assertEquals(data.getClusters().iterator().next().getElements().get(0).getChanges().size(),
                parsedData.getClusters().iterator().next().getElements().get(0).getChanges().size());
        assertEquals(data.getClusters().iterator().next().getElements().get(0).getChanges().get(0),
                parsedData.getClusters().iterator().next().getElements().get(0).getChanges().get(0));
    }

    @Test
    public void testSolutionClusterTransformation() throws Exception {
        final var cluster = new Cluster<>(Arrays.asList(
                new Solution("some code", "1", "1", OK),
                new Solution("some other code", "2", "4", OK),
                new Solution("some another code", "3", "6", OK)
        ));
        final var parsedCluster = SerializationTest.writeAndRead(cluster,
                EntityToProtoUtils::transformSolutionsCluster,
                ProtoToEntityUtils::transform,
                ProtoCluster::writeTo,
                ProtoCluster::parseFrom);
        assertArrayEquals(cluster.elementsCopy().toArray(), parsedCluster.elementsCopy().toArray());
    }

    @Test
    public void testSolutionClustersTransformation() throws Exception {
        final var clusters = new Clusters<>(Arrays.asList(
                new Cluster<>(Arrays.asList(
                        new Solution("some code1", "1", "1", OK),
                        new Solution("some code2", "2", "4", OK),
                        new Solution("some code3", "3", "6", OK))),
                new Cluster<>(Arrays.asList(
                        new Solution("some code4", "4", "8", OK),
                        new Solution("some code5", "5", "10", OK),
                        new Solution("some code6", "6", "12", OK)))));
        final var parsedClusters = SerializationTest.writeAndRead(clusters,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoClusters::writeTo,
                ProtoClusters::parseFrom);
        final var clustersElements = clusters.getClusters();
        final var parsedClustersElements = parsedClusters.getClusters();
        assertEquals(clustersElements.size(), parsedClustersElements.size());
        final var original = parsedClustersElements.iterator();
        final var parsed = clustersElements.iterator();
        while (original.hasNext()) {
            final var originalElement = original.next();
            final var parsedElement = parsed.next();
            assertArrayEquals(originalElement.elementsCopy().toArray(), parsedElement.elementsCopy().toArray());
        }
    }

    @Test
    public void testMarkedClustersTransformation() throws Exception {
        final List<Solution> solutions = Arrays.asList(
                new Solution("some code1", "1", "1", OK),
                new Solution("some code2", "2", "4", OK),
                new Solution("some code3", "3", "6", OK),
                new Solution("some code4", "4", "8", OK),
                new Solution("some code5", "5", "10", OK),
                new Solution("some code6", "6", "12", OK));
        final var clusters = new MarkedClusters<>(Map.of(
                new Cluster<>(solutions.subList(0, 3)),
                "mark1",
                new Cluster<>(solutions.subList(3, 6)),
                "mark2"));
        final var parsedClusters = SerializationTest.writeAndRead(clusters,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoMarkedClusters::writeTo,
                ProtoMarkedClusters::parseFrom);
        final Map<Solution, String> marks = parsedClusters.getFlatMarks();
        assertEquals("mark1", marks.get(solutions.get(0)));
        assertEquals("mark1", marks.get(solutions.get(1)));
        assertEquals("mark1", marks.get(solutions.get(2)));
        assertEquals("mark2", marks.get(solutions.get(3)));
        assertEquals("mark2", marks.get(solutions.get(4)));
        assertEquals("mark2", marks.get(solutions.get(5)));
    }

    @Test
    public void testSolutionMarksHolderTransformation() throws Exception {
        final Solution solution1 = new Solution("", "1", "1", FAIL);
        final Solution solution2 = new Solution("", "2", "4", FAIL);
        final Solution solution3 = new Solution("", "3", "6", FAIL);

        final var holder = new SolutionMarksHolder();
        holder.addMark(solution1, "mark11");
        holder.addMark(solution1, "mark12");
        holder.addMark(solution2, "mark21");
        holder.addMark(solution3, "mark31");
        holder.addMark(solution3, "mark32");
        holder.addMark(solution3, "mark33");
        final var parsedHolder = SerializationTest.writeAndRead(holder,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoSolutionMarksHolder::writeTo,
                ProtoSolutionMarksHolder::parseFrom);
        assertArrayEquals(holder.getMarks(solution1).map(List::toArray).get(),
                parsedHolder.getMarks(solution1).map(List::toArray).get());
        assertArrayEquals(holder.getMarks(solution2).map(List::toArray).get(),
                parsedHolder.getMarks(solution2).map(List::toArray).get());
        assertArrayEquals(holder.getMarks(solution3).map(List::toArray).get(),
                parsedHolder.getMarks(solution3).map(List::toArray).get());
    }

    @Test
    public void testDatasetTransformation() throws Exception {
        final Solution solution1 = new Solution("", "1", "1", FAIL);
        final Solution solution2 = new Solution("", "2", "4", FAIL);
        final Solution solution3 = new Solution("", "3", "6", FAIL);
        final Dataset dataset = new Dataset(Arrays.asList(solution1, solution2, solution3));

        final var parsedDataset = SerializationTest.writeAndRead(dataset,
                EntityToProtoUtils::transform,
                ProtoToEntityUtils::transform,
                ProtoDataset::writeTo,
                ProtoDataset::parseFrom);
        final List<Solution> data = parsedDataset.getValues();
        assertEquals(3, data.size());
        assertEquals(data.get(0), solution1);
        assertEquals(data.get(1), solution2);
        assertEquals(data.get(2), solution3);
    }
}
