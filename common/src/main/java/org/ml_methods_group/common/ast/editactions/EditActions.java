package org.ml_methods_group.common.ast.editactions;

import org.ml_methods_group.common.Solution;

//import jdk.nashorn.internal.runtime.regexp.joni.constants.NodeType;

import org.ml_methods_group.common.ast.NodeType;

import com.github.gumtreediff.actions.model.Action;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import com.github.gumtreediff.tree.ITree;


public class EditActions  implements Serializable {
    private final Solution origin;
    private final Solution target;
    private final List<Action> actions;

    public EditActions(Solution origin, Solution target, List<Action> actions) {
        this.origin = origin;
        this.target = target;
        this.actions = actions;
    }

    public Solution getOrigin() {
        return origin;
    }

    public Solution getTarget() {
        return target;
    }

    public List<Action> getEditActions() {
        return actions;
    }

    public static String getActionName(Action action){
        return action.toString() + " " + action.getNode().getParent().getType() + "@@";
    }
    

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EditActions changes = (EditActions) o;
        return origin.equals(changes.origin) && target.equals(changes.target);
    }
}
