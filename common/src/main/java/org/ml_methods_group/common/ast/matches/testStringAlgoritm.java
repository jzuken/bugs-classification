package org.ml_methods_group.common.ast.matches;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import org.apache.commons.lang.StringUtils;

import org.ml_methods_group.common.ast.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;


public final class testStringAlgoritm {
    private testStringAlgoritm() {}


    public static boolean isSimilarType(ITree A, ITree B){

        if(A.hasSameType(B)) return true;
        if(A.getType() ==-1 && B.getType()==-1) return true;
        if(A.getType() ==-1 || B.getType()==-1) return false;
        
        
        NodeType tA = NodeType.valueOf(A.getType());
        NodeType tB = NodeType.valueOf(A.getType());
        if(
            (tA == NodeType.C_DO || tA == NodeType.C_FOR || tA == NodeType.C_WHILE  )
            &&
            (tB == NodeType.C_DO || tB == NodeType.C_FOR || tB == NodeType.C_WHILE  )
        )
            return true;

        if(
            (tA == NodeType.C_IF_STMT || tA == NodeType.C_SWITCH   )
            &&
            (tB == NodeType.C_IF_STMT || tB == NodeType.C_SWITCH  )
        )
            return true;

        if(
            (tA == NodeType.C_RETURN || tA == NodeType.C_EXPR_STMT   )
             &&
            (tB == NodeType.C_RETURN || tB == NodeType.C_EXPR_STMT  )
            )
                return true;

        if(
            (tA == NodeType.C_ELSE || tA == NodeType.C_DEFAULT   )
            &&
            (tB == NodeType.C_ELSE || tB == NodeType.C_DEFAULT  )
        )
            return true;       
            
        if(
            (tA == NodeType.C_TYPE || tA == NodeType.C_STRUCT_DECL ||  tA == NodeType.C_UNION  )
            &&
            (tB == NodeType.C_TYPE || tB == NodeType.C_STRUCT_DECL ||  tB == NodeType.C_UNION  )
        )
            return true; 
        
        return false;
    }
    

    private static boolean InArray( String test, String[] array){
        List<String> list = Arrays.asList(array);

        if(list.contains(test)){
            return true;
        }
        return false;
    }

    private static final String[] compareOP = new String[]{"<", ">", "==",">=", "<="};
    private static final String[] logicalOP = new String[]{"&&", "||","&","|","^"};
    private static final String[] mathOP = new String[]{"+", "-", "*", "/"};
    private static final String[] mathUnaryOP = new String[]{"++", "--"};
    private static final String[] setOP = new String[]{"=", "+=","-=","*=","/="};
    private static final String[] braceOP = new String[]{"{", "}","(",")","[","]"};
    private static final String[] structOP = new String[]{".", "->", ";"};

    public static boolean isEql(ITree a, ITree b){
        if(isSimilarType(a,b)){

            if(!a.hasLabel() && !b.hasLabel()) 
            return true;

            NodeType tA = NodeType.valueOf(a.getType());

            if(tA == NodeType.C_COMMENT )
                return true;

            
            if(a.hasLabel() && b.hasLabel()) {

                String la =a.getLabel().toLowerCase();
                String lb =b.getLabel().toLowerCase();


                if(tA ==NodeType.C_OPERATOR){
                    if(InArray(la, compareOP) && InArray(lb, compareOP))   
                        return true;
                    if(InArray(la, logicalOP) && InArray(lb, logicalOP))   
                        return true;
    
                    if(InArray(la, mathOP) && InArray(lb, mathOP))   
                        return true;
                    if(InArray(la, mathUnaryOP) && InArray(lb, mathUnaryOP))   
                        return true;
                    if(InArray(la, setOP) && InArray(lb, setOP))   
                        return true;
                    if(InArray(la, braceOP) && InArray(lb, braceOP))   
                        return true;
                    if(InArray(la, structOP) && InArray(lb, structOP))   
                        return true;
                }

                
                if(  tA ==NodeType.C_NAMESPACE ||  tA ==NodeType.C_LABEL  ){
                    if (la.contains(lb))
                        return true;
                    if (lb.contains(la))
                        return true;
                    //if(longestSubString(la,lb) >=5)
                    //    return true;

                }

                if( tA ==NodeType.C_NAME){
                    if (la.contains(lb))
                        return true;
                    if (lb.contains(la))
                        return true;
                    //if(longestSubString(la,lb) >=5)
                    //    return true;
                }


               if( tA ==NodeType.C_LITERAL ){
                
                  if(longestSubString(la,lb) >=3)
                       return true;
               }

                if(tA ==NodeType.C_SPECIFIER)
                    return true;


                if(tA !=NodeType.C_OPERATOR &&  tA !=NodeType.C_NAME ){
                   // System.out.println("!=  -> " + tA.name()+" a:" + a.getLabel() + " b:" + b.getLabel());
                }

               
                
            }
        }
        return false;
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


    public static int longestSubString(String s0, String s1) {
        int maxLen=0;
        int[][] lengths = new int[s0.length() + 1][s1.length() + 1];
        for (int i = 0; i < s0.length(); i++){
            for (int j = 0; j < s1.length(); j++){
                if (s0.charAt(i)==s1.charAt(j))    
                    lengths[i + 1][j + 1] = lengths[i][j] + 1;
                if(lengths[i + 1][j + 1] >maxLen)
                    maxLen=lengths[i + 1][j + 1];
            }
        }
        return maxLen;

    }


    
}
