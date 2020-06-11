package org.ml_methods_group.common.ast.normalization;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.ast.NodeType;
import org.ml_methods_group.common.ast.changes.MetadataKeys;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NamesASTNormalizer extends BasicASTNormalizer {
    public void normalize(TreeContext context, String code) {
        NamesProcessor processor = new NamesProcessor(context, code);
        context.setRoot(processor.visit(context.getRoot()));
        context.validate();
    }

    private static class NamesProcessor extends StructureProcessor {
        private final ArrayDeque<Map<String, String>> aliases = new ArrayDeque<>();
        private final Map<String, Integer> totalCounters = new HashMap<>();
        private final ArrayDeque<Map<String, Integer>> counters = new ArrayDeque<>();
        private final String code;

        private final Set<String> cNames = new HashSet<>();

        NamesProcessor(TreeContext context, String code) {
            super(context, code);
            this.code = code;
            aliases.add(new HashMap<>());
            counters.add(new HashMap<>());
        }

        void register(String name) {
            assert !typeDeclarations.isEmpty();
            register(name, typeDeclarations.peekLast());
        }

        void registerAsArray(String name, int arity) {
            assert !typeDeclarations.isEmpty();
            final String type = IntStream.range(0, arity)
                    .mapToObj(x -> "[]")
                    .collect(Collectors.joining("", typeDeclarations.peekLast(), ""));
            register(name, type);
        }

        void register(String name, String type) {
            super.register(name, type);
            final int id = totalCounters.compute(type, (t, count) -> (count == null ? 0 : count) + 1);
            counters.peekLast().compute(type, (t, count) -> (count == null ? 0 : count) + 1);
            final String alias = type + "@" + id;
            aliases.peekLast().put(name, alias);
        }

        private String getVariableAlias(String name) {
            final Iterator<Map<String, String>> iterator = aliases.descendingIterator();
            while (iterator.hasNext()) {
                final String alias = iterator.next().get(name);
                if (alias != null) {
                    return alias;
                }
            }
            return null;
        }

        void pushLayer() {
            super.pushLayer();
            aliases.addLast(new HashMap<>());
            counters.addLast(new HashMap<>());
        }

        void popLayer() {
            super.popLayer();
            aliases.pollLast();
            for (Map.Entry<String, Integer> entry : counters.pollLast().entrySet()) {
                totalCounters.compute(entry.getKey(), (t, count) -> count - entry.getValue());
            }
        }

        @Override
        protected ITree visitMyVariableName(ITree node) {
            final String oldLabel = node.getLabel();
            node.setLabel(getVariableAlias(oldLabel));
            node.setMetadata(MetadataKeys.ORIGINAL_NAME, oldLabel);
            return super.visitMyVariableName(node);
        }

        @Override
        protected ITree visitCBlock(ITree node) {
            pushLayer();
            var result = super.visitCBlock(node);
            popLayer();
            return result;
        }

        @Override
        protected ITree visitCName(ITree node) {
            int type = node.getParent().getType();
            List<Integer> wrongTypes =
                    List.of(
                            NodeType.C_TYPE,
                            NodeType.C_FUNCTION,
                            NodeType.C_CALL
                    ).stream().map(NodeType::getId).collect(Collectors.toList());
            if (!wrongTypes.contains(type)) {
                if (node.getParent().getType() == NodeType.C_DECL.getId()
                        && node.getParent().getChild(0).getType() == NodeType.C_TYPE.getId()) {
                    List<String> typeParts = node.getParent().getChild(0).getChildren().stream().map(ITree::getLabel).collect(Collectors.toList());
                    String typeLabel = String.join("_", typeParts);
                    register(node.getLabel(), typeLabel);
                }

                if (getVariableAlias(node.getLabel()) != null) {
                    node.setLabel(getVariableAlias(node.getLabel()));
                }

            }
            return super.visitCName(node);
        }
    }
}
