package org.ml_methods_group.common.ast.generation;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.ast.normalization.ASTNormalizer;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedASTGenerator extends BasicASTGenerator {

    private final Map<Solution, SoftReference<TreeContext>> cache = new ConcurrentHashMap<>();

    public CachedASTGenerator(ASTNormalizer normalizer) {
        super(normalizer);
    }

    public CachedASTGenerator() {
    }

    @Override
    public TreeContext buildTreeContext(Solution solution) {
        if (solution.getSolutionId() == "-1") {
            return super.buildTreeContext(solution);
        }
        final SoftReference<TreeContext> reference = cache.get(solution);
        final TreeContext cached = reference == null ? null : reference.get();
        if (cached != null) {
            return cached;
        }
        final TreeContext tree = super.buildTreeContext(solution);
        cache.put(solution, new SoftReference<>(tree));
        return tree;
    }

    @Override
    public ITree buildTree(Solution solution) {
        try {
            
            final TreeContext context;
            context = buildTreeContext(solution);
            return context.getRoot().deepCopy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
