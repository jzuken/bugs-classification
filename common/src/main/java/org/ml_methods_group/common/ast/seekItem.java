package org.ml_methods_group.common.ast;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class seekItem {
    public String actionString;
    public String seekString;
    public seekItem(String _seek, String _action){
        this.actionString =_action;
        this.seekString =_seek;
    }
}
