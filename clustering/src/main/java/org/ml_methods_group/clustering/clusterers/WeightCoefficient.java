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
import org.ml_methods_group.common.ast.normalization.BasicASTNormalizer;
import org.ml_methods_group.common.ast.normalization.NamesASTNormalizer;

import java.util.List;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class WeightCoefficient {

    Changes defectA;
    Changes defectB;

    public WeightCoefficient(Changes defectA, Changes defectB) {
        this.defectA = defectA;
        this.defectB = defectB;
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

        ASTGenerator generator = new CachedASTGenerator(  new NamesASTNormalizer() ); // abstract
//        ASTGenerator generator = new CachedASTGenerator( new BasicASTNormalizer() ); // conctrete

        TreeContext srcA = generator.buildTreeContext(fromSolutionA);
        TreeContext srcB = generator.buildTreeContext(fromSolutionB);
        TreeContext dstB = generator.buildTreeContext(toSolution);

        TreeContext mSrc = new TreeContext();

        Matcher matcherAst = Matchers.getInstance().getMatcher(srcA.getRoot(), dstB.getRoot());
        try {
            matcherAst.match();
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }

        ActionGenerator actionGenerator = new ActionGenerator(srcA.getRoot(), dstB.getRoot(), matcherAst.getMappings());
        try{
            actionGenerator.generate();
        } catch (Exception e){
            System.out.println( e.getMessage());
            e.printStackTrace();
        }


        final List<Action> actions = actionGenerator.getActions();

        if(srcA != null && dstB != null  && actions !=null ){
            if(  actions.size() >0) {
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
                int dstBSize = dstB.getRoot().getSize();

                int mSrcSize = mSrc.getRoot().getSize();

                result = (double)(mSrcSize * 2) / (srcASize + srcBSize);

                mSrc = new TreeContext();
                srcA = new TreeContext();
                srcB = new TreeContext();
                dstB = new TreeContext();
                return result;
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());

            }
        }

        return 1;
    }

}
