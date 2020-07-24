package org.ml_methods_group.common.ast.generation;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.srcml.SrcmlCTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.ast.normalization.ASTNormalizer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BasicASTGenerator implements ASTGenerator {

    private final ASTNormalizer normalizer;
    private final TreeGenerator generator;

    public BasicASTGenerator(ASTNormalizer normalizer) {
        this.normalizer = normalizer;
        generator = new SrcmlCTreeGenerator();
    }

    public BasicASTGenerator() {
        this(null);
    }

    public static Map<Integer, String> nodeTypes = new HashMap<>();


    @Override
    public TreeContext buildTreeContext(Solution solution) {
        try {
            final String code = solution.getCode();
            final TreeContext context;
            context = generator.generateFromString(code);

            List<ITree> currentNodes = new ArrayList<ITree>();
            currentNodes.add(context.getRoot());

            Set<Integer> types = new HashSet<>();

            while (!currentNodes.isEmpty()) {
                types.addAll(
                        currentNodes.stream().map(ITree::getType).collect(Collectors.toSet()));

                currentNodes = currentNodes.stream().map(ITree::getChildren).flatMap(List::stream)
                        .collect(Collectors.toList());
            }

            for(Integer type: types){
                if(!nodeTypes.containsKey(type)){
                    nodeTypes.put(type, context.getTypeLabel(type));
                }
            }

            if (normalizer != null) {
                normalizer.normalize(context, code);
            }
            return context;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ITree buildTree(Solution solution) {
        try {
            
            final TreeContext context;
            context = buildTreeContext(solution);
            return context.getRoot();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
