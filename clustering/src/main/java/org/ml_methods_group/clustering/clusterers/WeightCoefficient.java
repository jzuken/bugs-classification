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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class WeightCoefficient {

    Changes defectA;
    Changes defectB;

    static Hashtable<String, TreeContext> contexts;
    static Hashtable<String, List<Action>> actionsTable;
    static List<String> result;
    String pathToMaxTreeDir;
    String version;


    public WeightCoefficient(Changes defectA, Changes defectB) {
        this.defectA = defectA;
        this.defectB = defectB;
        if (contexts == null) contexts = new Hashtable<>();
        if (actionsTable == null) actionsTable = new Hashtable<>();

        this.pathToMaxTreeDir = "C:\\Users\\kWX910209\\Documents\\gumtree_csv\\maxtreeAllJavaConctrete";
        this.version = "conctrete";

        if (result == null) {
            try {
                //            get all the files in MaxTree directory
                this.result = Files.walk(Paths.get(pathToMaxTreeDir.toString())).filter(Files::isRegularFile)
                        .map(x -> x.toString()).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

//    calculate from scratch
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
// calculate from pre-generated files
    public double calculate2() {

        String DefectA = defectA.getOrigin().getId();
        String DefectB = defectB.getOrigin().getId();

        try {

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


                if (filenameParts[0].equals("src") && filenameParts[2].equals(version)) {
                    if (filenameParts[1].equals(DefectA)) {
                        defectAPath = fName;
                    } else if (filenameParts[1].equals(DefectB)) {
                        defectBPath = fName;
                    }

                }

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

//                System.out.println("---");
//                System.out.println((float) c / ab);

                return (float) c / ab;
            }


            return 1;

        } catch (IOException e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }

// return 1 by default which won't affect initial value on multiplication
        return 1;
    }

}
