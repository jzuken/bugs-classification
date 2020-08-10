package org.ml_methods_group.common.ast.editactions;


import org.ml_methods_group.common.ast.NodeType;

import com.github.gumtreediff.actions.model.Action;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import com.github.gumtreediff.tree.ITree;


public class ActionContext {

   

    public static String GetContextPath(Action a){
        ITree n = a.getNode();
        String nodePath = NodeType.valueOf(n.getType()).name();
        while( ! IsContextRoot(n) ){
            n = n.getParent();
            nodePath = NodeType.valueOf(n.getType()).name() +"\\" + nodePath;
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
        // first noe in tree !
        if(n.getParent() ==null)  
            return true; 

        NodeType nt  =  NodeType.valueOf(n.getType());
        if(
            nt == NodeType.C_DECL_STMT || 
            nt == NodeType.C_SWITCH || 
            nt == NodeType.C_IF_STMT || 
            nt == NodeType.C_DO ||
            nt == NodeType.C_FUNCTION_DECL ||
            nt == NodeType.C_INCLUDE ||
            nt == NodeType.C_STRUCT ||
            nt == NodeType.C_TYPE ||
            nt == NodeType.C_FOR ||
            nt == NodeType.C_LABEL ||
            nt == NodeType.C_STRUCT ||
            nt == NodeType.C_MACRO ||
            nt == NodeType.C_NAMESPACE 
        )
            return true;

        return false;

    }
}