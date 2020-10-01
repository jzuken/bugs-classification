package org.ml_methods_group.common.ast;
import java.io.Serializable;

public class seekItem   implements Serializable {

    static final long serialVersionUID=1;

    public String actionString;
    public String seekString;
    
    // itemType for feature use, for manual markers e.t.c
    public String itemType;

    public seekItem(String _seek, String _action){
        this.actionString =_action;
        this.seekString =_seek;
        this.itemType = "C";   // C - change, R - reason, ???
    }

    
    public seekItem(String _seek, String _action, String _type){
        this.actionString =_action;
        this.seekString =_seek;
        this.itemType = _type;

    }
}
