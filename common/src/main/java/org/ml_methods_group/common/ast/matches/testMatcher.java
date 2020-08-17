package org.ml_methods_group.common.ast.matches;

// import com.github.gumtreediff.utils.StringAlgorithms;
import org.ml_methods_group.common.ast.matches.testStringAlgoritm;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Register;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import java.util.HashSet;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class testMatcher  extends Matcher {

    public testMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    public void match() {
        List<ITree> srcSeq = TreeUtils.preOrder(src);
        List<ITree> dstSeq = TreeUtils.preOrder(dst);
        List<int[]> lcs = testStringAlgoritm.doIt(srcSeq, dstSeq);
        System.out.println("testMatcher found " + lcs.size() +" items");
        for (int[] x: lcs) {

            ITree t1 = srcSeq.get(x[0]);
            ITree t2 = dstSeq.get(x[1]);
            addMapping(t1, t2);
        }
    }



    protected int numberOfCommonChildren(ITree src, ITree dst) {
        Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        int common = 0;

        ITree m0 = mappings.getDst(src);
        if (m0 != null && dstDescendants.contains(m0)){
            for (ITree t : src.getChildren()) {
                ITree m = mappings.getDst(t);
                if (m != null && dstDescendants.contains(m)){
                    common++;
                    common += numberOfCommonChildren(t,dst);
                }
            }
        }
        if(common >0)
            System.out.println("Node " + src.getId() +" has " + common + " common descendants");
        return common;
    }


    protected int numberOfChildren(ITree src) {
        
        //System.out.println("Node " + src.getId() +" has " + src.getDescendants().size() + " descendants");
        return src.getDescendants().size();
    }

    protected boolean IsCommonNode(ITree src,  Set<ITree> dstDescendants){
        ITree m = mappings.getDst(src);
        if (m != null && dstDescendants.contains(m)){
            return true;
        }
        return false;
    }

    public ITree GetLongestSrcSubtree(){
        List<ITree> srcSeq = TreeUtils.preOrder(src);
        int[] c = new int[srcSeq.size() + 1];

        // считаем количество общих потомков для каждого узла
        for (int i = 0; i < srcSeq.size(); i++){
            c[i]=numberOfCommonChildren(srcSeq.get(i), dst);
        }

        int maxIdx=0;

        // находим узел с максимальным количеством совпадений
        for (int i = 1; i < srcSeq.size(); i++){
            if(c[maxIdx] <= c[i]) maxIdx= i;
        }

        System.out.println("longest SRC matches: " + c[maxIdx] );
        System.out.println("longest node id: " + srcSeq.get(maxIdx).getId() );
        System.out.println("Node has " + numberOfChildren(srcSeq.get(maxIdx)) + " dependents childrens" );

        ITree longestRoot = srcSeq.get(maxIdx);
        System.out.println("longest SRC subtree size: " + longestRoot.getSize() );
        
        // надо убрать все  узлы, которые не входят в искомое дерево
        longestRoot= removeUnmappedSrcNodes(longestRoot,dst);

        longestRoot.setParent(null);
        longestRoot.refresh();
        
        System.out.println("longest SRC subtree size after clean: " + longestRoot.getSize() );

        return longestRoot.deepCopy();
    }


    public ITree GetLongestDstSubtree(){
        List<ITree> dstSeq = TreeUtils.preOrder(dst);
        int[] c = new int[dstSeq.size() + 1];

        // считаем количество общих потомков для каждого узла
        for (int i = 0; i < dstSeq.size(); i++){
            c[i]=numberOfCommonChildren(dstSeq.get(i), src);
        }

        int maxIdx=0;

        // находим узел с максимальным количеством совпадений
        for (int i = 1; i < dstSeq.size(); i++){
            if(c[maxIdx] < c[i]) maxIdx= i;
        }

        System.out.println("longest DST matches: " + c[maxIdx] );

        ITree longestRoot = dstSeq.get(maxIdx).deepCopy();

        System.out.println("longest DST subtree size: " + longestRoot.getSize() );
        
        // надо убрать все  узлы, которые не входят в искомое дерево
        longestRoot = removeUnmappedDstNodes(longestRoot);
        System.out.println("longest DST subtree size after clean: " + longestRoot.getSize() );

        return longestRoot;
    }

    public ITree  removeUnmappedSrcNodes(ITree node, ITree dst){

        Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        
        

        List<ITree> children = node.getChildren();
        List<ITree> toRemove = new ArrayList<ITree>();

        System.out.println("Removeing extra childern for " + node.getId() + ", checking " + children.size() + " childrens");
        for (ITree t : children) {
            if ( numberOfCommonChildren(t,dst) == 0 ){
                System.out.println("Child " + t.getId() + " will be removed");
                toRemove.add(t);
            }
        }

        System.out.println("Prepare to remove " +toRemove.size() + " items");

         // removing nodes
        for (ITree t : toRemove) {
            children.remove(t);
        }


        System.out.println("Node has " +children.size() + " childrens");

        node.setChildren(children);
        

        for (ITree t : children ) {
            t = removeUnmappedSrcNodes(t, dst);
        }

        return node;

    }

    public ITree  removeUnmappedDstNodes(ITree node){

        List<ITree> children = node.getChildren();
        List<ITree> toRemove = new ArrayList<ITree>();

        // collect nodes to remove
        for (ITree t : children) {
            if (mappings.getDst(t) == null ){
                toRemove.add(t);
            }
        }

        // removing nodes
        for (ITree t : toRemove) {
            children.remove(t);
        }

        node.setChildren(children);

        for (ITree t : node.getChildren()) {
            removeUnmappedDstNodes(t);
        }
        return node;

    }

}