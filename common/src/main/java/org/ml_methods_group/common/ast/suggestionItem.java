package org.ml_methods_group.common.ast;

import java.io.Serializable;

public class suggestionItem  implements Serializable{

    static final long serialVersionUID=1;
    
    public  suggestionItem (int _nodeId, int _start, int _end, String _content, String _reson){
        this.nodeId =_nodeId;
        this.startPosition =_start;
        this.endPosition =_end;
        this.SuggestionContent =_content;
        this.reson =_reson;
    }

    public int nodeId;  // node  from tested source

    // position in source file
    public int startPosition; // start 
    public int endPosition;   // end  
    
    public String SuggestionContent;  

    public String reson;  // edit action path

}
