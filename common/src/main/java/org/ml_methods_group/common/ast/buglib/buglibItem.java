package org.ml_methods_group.common.ast.buglib;
import org.ml_methods_group.common.ast.seekItem;


import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

public class buglibItem  implements Serializable {
    static final long serialVersionUID=1;

    public Map<String,seekItem> markers;
    public String bugID; // defect id
    public String bugDescription;  // description of fixed bug
    
    public buglibItem(String _bugID, String _bugDescription, Map<String,seekItem> _markers) {
        this.bugID = _bugID;
        this.bugDescription = _bugDescription;
        this.markers =  new HashMap<String,seekItem>();
        this.markers.putAll(_markers);
        // System.out.println(_bugID + " markers count: " + this.markers.size());
    }
}
