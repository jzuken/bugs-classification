package org.ml_methods_group.common.ast.matches;

// import com.github.gumtreediff.utils.StringAlgorithms;
// import org.ml_methods_group.common.ast.matches.testStringAlgoritm;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.actions.model.Action;
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


    /* protected boolean NodeInActions(ITree src,  List<Action> actions){
        boolean yes =false;
        ITree m = mappings.getDst(src);
        if(m != null){
            for(Action a : actions){

                if(a.getNode().getId() == m.getId())
                    return true;

                for( ITree c : src.getDescendants()){
                    ITree n = mappings.getDst(c);
                    if(n!=null){
                        if(a.getNode().getId() == n.getId())
                            return true;
                    }
                }
            }
        }
        return yes;
    } */
    


    protected boolean NodeInActions(ITree src,  List<Action> actions){
        boolean yes =false;
    
        if(src != null){
            for(Action a : actions){

                if(a.getNode().getId() == src.getId())
                    return true;

                for( ITree c : src.getDescendants()){
                    if(c!=null){
                        if(a.getNode().getId() == c.getId())
                            return true;
                    }
                }
            }
        }
        return yes;
    }

    protected int numberOfCommonChildren(ITree src, Set<ITree> dstDescendants ) {
        // Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        int common = 0;


        ITree m0 = mappings.getDst(src);
        if (m0 != null && dstDescendants.contains(m0)){
            for (ITree t : src.getChildren()) {
                ITree m = mappings.getDst(t);
                if (m != null && dstDescendants.contains(m)){
                    common++;
                    common += numberOfCommonChildren(t, dstDescendants);
                }
            }
        }
        //if(common >0)
        //    System.out.println("Node " + src.getId() +" has " + common + " common descendants");
        return common;
    }

  
  
    public ITree GetLongestSrcSubtree(List<Action> actions){
        List<ITree> srcSeq = TreeUtils.preOrder(src);
        Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        int[] c = new int[srcSeq.size() + 1];
        

        // считаем количество общих потомков для каждого узла
        for (int i = 0; i < srcSeq.size(); i++){
            c[i]=numberOfCommonChildren(srcSeq.get(i), dstDescendants);
        }

        for (int i = 0; i < srcSeq.size(); i++){
            if(!NodeInActions( srcSeq.get(i), actions))
                c[i]=0;
        }

        int maxIdx=0;

        // находим узел с максимальным количеством совпадений
        for (int i = 1; i < srcSeq.size(); i++){
            if(c[maxIdx] <= c[i]) maxIdx= i;
        }

        System.out.println("longest matches: " + c[maxIdx] );
        System.out.println("longest node id: " + srcSeq.get(maxIdx).getId() );
       
        ITree longestRoot = srcSeq.get(maxIdx);
        System.out.println("longest subtree size before clean: " + longestRoot.getSize() );
        
        // Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());

        // надо убрать все  узлы, которые не входят в искомое дерево
        longestRoot= removeUnmappedSrcNodes(longestRoot, dstDescendants);

        longestRoot.setParent(null);
        longestRoot.refresh();
        
        System.out.println("longest subtree size after clean: " + longestRoot.getSize() );

        return longestRoot.deepCopy();
    }


    protected ITree  removeUnmappedSrcNodes(ITree node,  Set<ITree> dstDescendants){

        //Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());

        List<ITree> children = node.getChildren();
        List<ITree> toRemove = new ArrayList<ITree>();

        //System.out.println("Remove extra childern for " + node.getId() + ", checking " + children.size() + " childrens");
        for (ITree t : children) {
            ITree m = mappings.getDst(t);
            if (m == null || ! dstDescendants.contains(m))
            {
                toRemove.add(t);
            }
        }

        // System.out.println("Prepare to remove " +toRemove.size() + " items");

         // removing nodes
        for (ITree t : toRemove) {
            children.remove(t);
        }


        //System.out.println("Node has " +children.size() + " childrens after remove");

        node.setChildren(children);
        

        for (ITree t : children ) {
            t = removeUnmappedSrcNodes(t,  dstDescendants);
        }

        return node;

    }



    public List<ITree> GetLongestForest(List<Action> actions){
        List<ITree> forest = new ArrayList<ITree>();
        List<ITree> srcSeq = TreeUtils.preOrder(src);
        Set<ITree> dstDescendants = new HashSet<>(dst.getDescendants());
        int[] c = new int[srcSeq.size() + 1];
        

        // считаем количество общих потомков для каждого узла
        for (int i = 0; i < srcSeq.size(); i++){
            c[i]=numberOfCommonChildren(srcSeq.get(i), dstDescendants);
        }

        for (int i = 0; i < srcSeq.size(); i++){
            if(!NodeInActions( srcSeq.get(i), actions))
                c[i]=0;
        }

        boolean itemFound = false;
        int forestSize=0;
        do{
            itemFound = false;
            int maxIdx=0;

            // находим узел с максимальным количеством совпадений
            for (int i = 1; i < srcSeq.size(); i++){
                if(c[maxIdx] <= c[i]) maxIdx= i;
            }

            if(c[maxIdx] > 0){
                itemFound =true;
                //System.out.println("longest matches: " + c[maxIdx] );
                //System.out.println("longest node id: " + srcSeq.get(maxIdx).getId() );
            
                ITree longestRoot = srcSeq.get(maxIdx);
                //System.out.println("longest subtree size before clean: " + longestRoot.getSize() );
            
                // exclude found node
                c[maxIdx] =0;

                // try to clean child-count array 
                List<ITree> dc =longestRoot.getDescendants();
               

                for (int i = 0; i < srcSeq.size(); i++){
                    int nId = srcSeq.get(i).getId();
                    for(ITree d:dc){
                        if( nId== d.getId() ){
                            c[i]=0;   
                            break;
                        }
                    }
                }


                // надо убрать все  узлы, которые не входят в искомое дерево
                longestRoot= removeUnmappedSrcNodes(longestRoot, dstDescendants);

                longestRoot.setParent(null);
                longestRoot.refresh();
                
                //System.out.println("longest subtree size after clean: " + longestRoot.getSize() );

                

                forest.add(longestRoot.deepCopy());
                
                forestSize+= (longestRoot.getSize());
               
            }
        }while(itemFound);

        System.out.println("found: " + forest.size() +" trees, contains " + forestSize +" nodes" );
        return forest;
    }



}