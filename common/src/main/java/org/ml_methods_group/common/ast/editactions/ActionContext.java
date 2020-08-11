package org.ml_methods_group.common.ast.editactions;


import org.ml_methods_group.common.ast.NodeType;

import com.github.gumtreediff.actions.model.Action;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;


public class ActionContext {

   private static String GetNodeName(ITree n, Boolean withLabel,TreeContext ctx){
   
    String curName="";
    if(n != null){
        curName += n.toPrettyString(ctx); //.replace("\r"," ").replace("\n"," ").replace("\t"," ")  ;
        /*
        if(n.getType()!=-1){
            try{
                curName = NodeType.valueOf(n.getType()).name() ;
                if(withLabel && n.hasLabel())
                    //curName += n.toShortString() ;
                    curName += "{"+ n.getLabel().replace("\r"," ").replace("\n"," ") +"}" ;
                
            }catch(Exception e){
                curName = "" + n.getType();
                if(withLabel  && n.hasLabel())
                    //curName += n.toShortString()  ;
                    curName +="{"+ n.getLabel().replace("\r"," ").replace("\n"," ") +"}";
            }
        }else{
            if(withLabel && n.hasLabel())
                //curName += n.toShortString()  ;
                curName = "{"+ n.getLabel().replace("\r"," ").replace("\n"," ") +"}" ;
        }
        */
    }
    return curName;
   }

    public static String GetContextPath(Action a, Boolean withLabel, TreeContext ctx){
        ITree n = a.getNode();
        String nodePath = GetNodeName(n,withLabel,ctx);
        
        while( ! IsContextRoot(n) ){
            n = n.getParent();
            nodePath =GetNodeName(n,withLabel,ctx) +"\\" + nodePath;
        }
        return nodePath;
    }

    public static ITree GetContextRoot(Action a){
        ITree n = a.getNode();
        while( ! IsContextRoot(n) ){
            n = n.getParent();
        }
        return n;
    }

    private static Boolean IsContextRoot(ITree n){
        if(n == null)  
        return true; 

        // first node in tree !
        if(n.isRoot() )  
        return true;

        // first node in tree !!
        if(n.getParent() ==null)  
            return true; 

        if(n.getType()==-1)
            return false;

        NodeType nt  =  NodeType.valueOf(n.getType());
        if(
            nt == NodeType.C_DECL_STMT || 
            nt == NodeType.C_IF_STMT || 
            nt == NodeType.C_SWITCH || 
            nt == NodeType.C_DO ||
            nt == NodeType.C_FOR ||
            nt == NodeType.C_WHILE ||
            nt == NodeType.C_FUNCTION_DECL ||
            // nt == NodeType.C_FUNCTION ||
            // nt == NodeType.C_INCLUDE ||
            // nt == NodeType.C_STRUCT ||
            nt == NodeType.C_IFDEF ||
            nt == NodeType.C_IFNDEF ||
            nt == NodeType.C_UNDEF ||
            nt == NodeType.C_TYPE ||
            nt == NodeType.C_TYPEDEF ||
            nt == NodeType.C_STRUCT_DECL ||
            nt == NodeType.C_UNION ||
            nt == NodeType.C_LABEL ||
            nt == NodeType.C_MACRO ||
            nt == NodeType.C_UNIT ||   // top file node
            nt == NodeType.C_EXTERN ||
            nt == NodeType.C_NAMESPACE 
            
        )
            return true;

        return false;

    }
}
