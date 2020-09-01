package org.ml_methods_group.clustering.clusterers;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.ast.changes.Changes;
import org.ml_methods_group.common.ast.generation.ASTGenerator;
import org.ml_methods_group.common.ast.generation.CachedASTGenerator;
import org.ml_methods_group.common.ast.matches.testMatcher;
import org.ml_methods_group.common.ast.normalization.NamesASTNormalizer;

import java.util.Hashtable;
import java.util.List;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class WeightCoefficient {

    Changes defectA;
    Changes defectB;

    static Hashtable<String, TreeContext> contexts;
    static Hashtable<String, List<Action>> actionsTable;

    public WeightCoefficient(Changes defectA, Changes defectB) {
        this.defectA = defectA;
        this.defectB = defectB;
        if (contexts == null) contexts = new Hashtable<>();
        if (actionsTable == null) actionsTable = new Hashtable<>();
    }

    public double calculate() {
        String defectAID = defectA.getOrigin().getId();
        String defectBID = defectB.getOrigin().getId();

        String defectACode = defectA.getOrigin().getCode();
        String defectBCode = defectB.getOrigin().getCode();
        String fixACode = defectA.getTarget().getCode();

        var fromSolutionA = new Solution(defectACode, defectAID, defectAID + "_" + FAIL.ordinal(), FAIL);
        var fromSolutionB = new Solution(defectBCode, defectBID, defectBID + "_" + FAIL.ordinal(), FAIL);

        var toSolution = new Solution(fixACode, defectAID, defectAID + "_" + OK.ordinal(), OK);

        ASTGenerator generator = new CachedASTGenerator(new NamesASTNormalizer()); // abstract
//        ASTGenerator generator = new CachedASTGenerator( new BasicASTNormalizer() ); // conctrete

        boolean containsA = contexts.contains(fromSolutionA.getId());
        TreeContext srcA = containsA
                ? contexts.get(fromSolutionA.getId())
                : generator.buildTreeContext(fromSolutionA);
        if (!containsA) contexts.put(fromSolutionA.getId(), srcA);

        boolean containsB = contexts.contains(fromSolutionB.getId());
        TreeContext srcB = containsB
                ? contexts.get(fromSolutionB.getId())
                : generator.buildTreeContext(fromSolutionB);
        if (!containsB) contexts.put(fromSolutionB.getId(), srcB);

        boolean containsС = contexts.contains(toSolution.getId());
        TreeContext dstB = containsС
                ? contexts.get(toSolution.getId())
                : generator.buildTreeContext(toSolution);
        if (!containsС) contexts.put(toSolution.getId(), dstB);

        TreeContext mSrc = new TreeContext();

        final List<Action> actions;
        String target = fromSolutionA.getId() + "__" + toSolution.getId();
        if (actionsTable.contains(target)) {
            actions = actionsTable.get(target);
        } else {

            Matcher matcherAst = Matchers.getInstance().getMatcher(srcA.getRoot(), dstB.getRoot());
            try {
                matcherAst.match();
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
            }

            ActionGenerator actionGenerator = new ActionGenerator(srcA.getRoot(), dstB.getRoot(), matcherAst.getMappings());
            try {
                actionGenerator.generate();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            actions = actionGenerator.getActions();
            actionsTable.put(target, actions);
        }

        // uncomment if memory becomes an issue
        // if (actionsTable.size() > 1000) {
        //     actionsTable.clear();
        // }
        // if (contexts.size() > 2000) {
        //     contexts.clear();
        // }

        if (srcA != null && dstB != null && actions != null) {
            if (actions.size() > 0) {
                testMatcher matcher = new testMatcher(srcA.getRoot(), dstB.getRoot(), new MappingStore());

                try {
                    matcher.match();
                } catch (NullPointerException e) {
                    System.out.println(e.getMessage());

                }

                ITree minSrc = matcher.GetLongestSrcSubtree(actions);


                mSrc.importTypeLabels(dstB);
                mSrc.setRoot(minSrc);
                mSrc.getRoot().refresh();
            }
        }

        if (mSrc != null) {
            try {
                double result = 0;
                int srcASize = srcA.getRoot().getSize();
                int srcBSize = srcB.getRoot().getSize();
                // int dstBSize = dstB.getRoot().getSize();

                int mSrcSize = mSrc.getRoot().getSize();

                result = (double) (mSrcSize * 2) / (srcASize + srcBSize);

                return result;
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());

            }
        }

        return 1;
    }

}
