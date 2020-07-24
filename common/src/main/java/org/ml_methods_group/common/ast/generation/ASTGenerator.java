package org.ml_methods_group.common.ast.generation;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.Solution;

import java.io.Serializable;

public interface ASTGenerator extends Serializable {
    ITree buildTree(Solution solution);
    TreeContext buildTreeContext(Solution solution);
}
