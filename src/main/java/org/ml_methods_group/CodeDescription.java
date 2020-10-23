package org.ml_methods_group;


import org.ml_methods_group.common.*;
import java.util.*;
 
 
 public class CodeDescription{
        public String ID;
        public List<String> words;
        public CodeDescription(String _ID, String Code){
            ID=_ID;
            words = Arrays.asList(Code.split("\n"));
        }
    }