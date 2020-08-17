package org.ml_methods_group.common.ast.matches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;

public final class testStringAlgoritm {
    private testStringAlgoritm() {}


    
    public static boolean isEql(ITree A, ITree B){
        Tree a = (Tree) A;
        Tree b = (Tree) B;
        Boolean y= false;
        if(a.hasSameType(b)){
            if(a.hasLabel() && b.hasLabel()) 
                y = true;
            if(!a.hasLabel() && !b.hasLabel()) 
                y = true;
        }
        return y;
    }

    public static List<int[]> doIt(List<ITree> s0, List<ITree> s1) {
        int[][] lengths = new int[s0.size() + 1][s1.size() + 1];
        for (int i = 0; i < s0.size(); i++)
            for (int j = 0; j < s1.size(); j++)
            if (isEql(s0.get(i),s1.get(j)))    
                lengths[i + 1][j + 1] = lengths[i][j] + 1;
            else
                lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);

        List<int[]> indexes = new ArrayList<>();

        for (int x = s0.size(), y = s1.size(); x != 0 && y != 0; ) {
            if (lengths[x][y] == lengths[x - 1][y]) x--;
            else if (lengths[x][y] == lengths[x][y - 1]) y--;
            else {
                indexes.add(new int[] {x - 1, y - 1});
                x--;
                y--;
            }
        }
        Collections.reverse(indexes);
        return indexes;
    }

    
}