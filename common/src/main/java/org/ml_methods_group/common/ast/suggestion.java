package org.ml_methods_group.common.ast;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class suggestion   implements Serializable {

    static final long serialVersionUID=1;
    
    public String BugLibraryItem; 
    public List<suggestionItem> suggestions;

    public suggestion(String bugLibDefect, List<suggestionItem> items) {
        this.BugLibraryItem = bugLibDefect;
        this.suggestions = items;
    }

    public suggestion(String bugLibDefect) {
        this.BugLibraryItem = bugLibDefect;
        this.suggestions = new ArrayList<suggestionItem>();
    }

}
