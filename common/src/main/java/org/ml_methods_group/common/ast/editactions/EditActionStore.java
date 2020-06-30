package org.ml_methods_group.common.ast.editactions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.TreeContext;
import org.ml_methods_group.common.ast.editactions.Encoder;
import com.github.gumtreediff.utils.Pair;
//import com.github.gumtreediff.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toMap;


public class EditActionStore{

    private static Boolean USE_BITSET = true;
    private Encoder encoder;

    //private Parameters parameters;
    private List<String> allChangeNamesList = new ArrayList<>();

    //private Map<String, Integer> ngramStats = new HashMap<>();
    //private Map<String, Map<String, Integer>> fromChangeToHist = new HashMap<>();

    private Map<BitSet, Integer> ngramStatsBitset = new HashMap<>();
    private Map<String, Map<BitSet, Integer>> fromChangeToHistBitset = new HashMap<>();

    private static boolean isNumeric(String strNum) {
        try {
             Integer.parseInt(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    public List<Pair<String, Map<BitSet, Integer>>> Representation(){
        List<Pair<String, Map<BitSet, Integer>>> r = new ArrayList();
        for (Map.Entry<String, Map<BitSet, Integer>> entry : fromChangeToHistBitset.entrySet()) {
            String name = entry.getKey();
            Map<BitSet, Integer> value = entry.getValue();
            r.add(new Pair<>(name, value));
        }  
        return r;
    }
    
    public EditActionStore() {
        encoder = new Encoder(true);
    }

    private static boolean needsContext(Action action) {
        return action.getName().equals("DEL") || action.getName().equals("UPD");
    }

    public static String clean(String action) {
        //System.out.println("clean.start");
        String[] tokens = action.split(" ");
        int n = tokens.length;

        String nodeTypeTag = "@@";
        String modificationType = tokens[0];

        List<String> result = new ArrayList<>();
        result.add(modificationType);

        for (int i = 1; i < n; i++) {
            String token = tokens[i];

            if (token.contains(nodeTypeTag)) {
                String[] splitted = token.split(nodeTypeTag);

                if (splitted.length == 0)
                    continue;

                if (isNumeric(splitted[0])) {
                    result.add(splitted[0] + nodeTypeTag);
                }
            } else {
                if (modificationType.equals("UPD") || modificationType.equals("DEL")) {
                    continue;
                } else {
                    if (token.equals("at"))
                        continue;
                }

                if (i == n - 1) {
                    result.add(token);
                }
            }
        }
        //System.out.println("clean.end");
        return String.join(" ", result);
    }

    /*
      Returns actions converted to list of strings: raw and processed.
     */
    public Pair<List<String>, List<String>> convertToStrings(List<Action> actions) {
       // System.out.println("convertToStrings.start");
        List<String> actionsRawStrings = new ArrayList<>();
        List<String> actionsStrings = new ArrayList<>();

        for (Action action : actions) {
            // if (parameters.getUseContext() && needsContext(action)) {
                //System.out.println("convertToStrings.1");
                actionsRawStrings.add(action.toString() + " " + action.getNode().getParent().getType() + "@@");
                //System.out.println("convertToStrings.2");
                actionsStrings.add(clean(action.toString()) + " " + action.getNode().getParent().getType() + "@@");
               // System.out.println("convertToStrings.3");
            /*} else {
                actionsRawStrings.add(action.toString());
                actionsStrings.add(clean(action.toString()));
            }*/
        }
       // System.out.println("convertToStrings.end");

        return new Pair<>(actionsRawStrings, actionsStrings);
    }

    
    
    public  List<BitSet> calcActionsNgram( List<String> actions, int n) {

        BitSet actionsBS = encoder.encode(actions);
        List<BitSet> ngrams = new ArrayList();
        int numActions =actions.size();
       
        if (numActions < n) {
            
            BitSet seq = encoder.getActionsSublist(actionsBS, 0, numActions);
            ngrams.add( seq);
        } else {
            int i = 0;

            while (i + n <= numActions) {
                BitSet seq = encoder.getActionsSublist(actionsBS, i, i + n);
                ngrams.add( seq);
                i++;
            }

        }
        return ngrams;
             
    }


    public  BitSet calcActionsBitSet( String action) {

        List<String> actions = new ArrayList();
        actions.add(action);
        BitSet actionsBS = encoder.encode(actions);
        return actionsBS;
    }

    private void addActions(String name, BitSet actions, int numActions) {

        //System.out.println("addActions: " + name);
        int n = 5; //parameters.getN();

        //switch (parameters.getRepresentationType()) {
        //    case SHORT_AS_NGRAM:
                if (numActions < n) {
                    //System.out.println("addActions: " + name + "[0.." + numActions +"]");
                    BitSet seq = encoder.getActionsSublist(actions, 0, numActions);
                    addNgramOfChange(name, seq);
                } else {
                    int i = 0;

                    while (i + n <= numActions) {
                       // System.out.println("addActions: " + name + "[" +i +".." + (i+n) +"]");
                        BitSet seq = encoder.getActionsSublist(actions, i, i + n);
                        addNgramOfChange(name, seq);

                        i++;
                    }

                }
         /*       break;
            case CONCAT:
                int k = 1;

                while (k <= Math.min(numActions, n)) {
                    int i = 0;

                    while (i + n <= numActions) {
                        BitSet seq = encoder.getActionsSublist(actions, i, i + k);
                        addNgramOfChange(name, seq);

                        i++;
                    }

                    k++;
                }
        }*/
    }

    public void addActions(String name, List<String> actions) {
        if (actions.isEmpty())
            return;

        if (!allChangeNamesList.contains(name)) {
            allChangeNamesList.add(name);
        }

        int numActions = actions.size();

        if (USE_BITSET) {
            BitSet actionsBitset = encoder.encode(actions);
            addActions(name, actionsBitset, numActions);
        } 
        
        /*else {
            int n = 5; //parameters.getN();

           // switch (parameters.getRepresentationType()) {
           //     case SHORT_AS_NGRAM:
                    if (numActions < n) {
                        String seq = String.join(" ", actions.subList(0, numActions));
                        addNgramOfChange(name, seq);
                    } else {
                        int i = 0;

                        while (i + n <= numActions) {
                            String seq = String.join(" ", actions.subList(i, i + n));
                            addNgramOfChange(name, seq);

                            i++;
                        }

                    }
                    */
         /*           break;
                case CONCAT:
                    int k = 1;

                    while (k <= Math.min(numActions, n)) {
                        int i = 0;

                        while (i + n <= numActions) {
                            String seq = String.join(" ", actions.subList(i, i + k));
                            addNgramOfChange(name, seq);

                            i++;
                        }

                        k++;
                    }
            } */
       // }
    }

   /* private void addNgramOfChange(String name, String ngram) {
        System.out.println("addNgramOfChange->" + name +" : " + ngram);
        if (!ngramStats.containsKey(ngram)) {
            ngramStats.put(ngram, 0);
        }
        int count = ngramStats.get(ngram);
        ngramStats.replace(ngram, count + 1);

        Map<String, Integer> histOfSample = fromChangeToHist.getOrDefault(name, new HashMap<>());
        histOfSample.merge(ngram, 1, Integer::sum);
        fromChangeToHist.put(name, histOfSample);
    }

    */
    private void addNgramOfChange(String name, BitSet ngram) {
       // System.out.println("addNgramOfChange2->" + name +" ng=" + ngram.toString());
        if (!ngramStatsBitset.containsKey(ngram)) {
            ngramStatsBitset.put(ngram, 0);
        }
        int count = ngramStatsBitset.get(ngram);
        ngramStatsBitset.replace(ngram, count + 1);

        Map<BitSet, Integer> histOfSample = fromChangeToHistBitset.getOrDefault(name, new HashMap<>());
        histOfSample.merge(ngram, 1, Integer::sum);
        fromChangeToHistBitset.put(name, histOfSample);
    }

   /* public void saveRepresentations(String pathToSave, TreeContext treeContext) throws IOException {
        if (USE_BITSET) {
            saveRepresentationsBitset(pathToSave, treeContext);
            return;
        }

        LinkedHashMap<String, Integer> sorted = ngramStats
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        List<Map.Entry<String, Integer>> commonStats = new ArrayList<>(sorted.entrySet());
        Map<String, Integer> editScriptToInd = new HashMap<>();
        for (int i = 0; i < commonStats.size(); i++) {
            String editScript = commonStats.get(i).getKey();
            editScriptToInd.put(editScript, i);
        }

        File editScriptsFileUnmapped = new File(Paths.get(pathToSave).resolve("ngrams_ids.txt").toString());
        editScriptsFileUnmapped.getParentFile().mkdirs();
        editScriptsFileUnmapped.createNewFile();
        BufferedWriter editScriptsWriterUnmapped = new BufferedWriter(new FileWriter(editScriptsFileUnmapped.getAbsolutePath()));
        int num = 0;
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            editScriptsWriterUnmapped.write(num + ") " + key + " = " + value + "\n");
            num++;
        }
        editScriptsWriterUnmapped.close();


        if (treeContext != null) {
            File editScriptsFileMapped = new File(Paths.get(pathToSave).resolve("ngrams_ids_with_tree_context.txt").toString());
            editScriptsFileMapped.getParentFile().mkdirs();
            editScriptsFileMapped.createNewFile();
            BufferedWriter editScriptsWriterMapped = new BufferedWriter(new FileWriter(editScriptsFileMapped.getAbsolutePath()));
            num = 0;
            for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
                String key = mapWithContext(entry.getKey(), treeContext);
                Integer value = entry.getValue();
                editScriptsWriterMapped.write(num + ") " + key + " = " + value + "\n");
                num++;
            }
            editScriptsWriterMapped.close();
        }


        int processed = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : fromChangeToHist.entrySet()) {
            System.out.println("Processed: " + processed + "/" + fromChangeToHist.entrySet().size());
            processed++;

            String name = entry.getKey();

            File histFile = new File(Paths.get(pathToSave).resolve(name).resolve("sparse_hist.txt").toString());

            histFile.getParentFile().mkdirs();
            histFile.createNewFile();
            BufferedWriter histWriter = new BufferedWriter(new FileWriter(histFile.getAbsolutePath()));

            Map<String, Integer> hist = entry.getValue();

            List<Pair<Integer, Integer>> sparseHist = new ArrayList<>();

            for (Map.Entry<String, Integer> histElem : hist.entrySet()) {
                Integer editScriptInd = editScriptToInd.getOrDefault(histElem.getKey(), -1);
                Integer height = histElem.getValue();
                sparseHist.add(new Pair<>(editScriptInd, height));
            }

            sparseHist.sort(Comparator.comparingInt(Pair::getFirst));

            for (Pair<Integer,Integer> pair : sparseHist) {
                histWriter.write(pair.getFirst() + " " + pair.getSecond() + "\n");
            }

            histWriter.close();
        }

    }
*/

    public void saveRepresentationsBitset(String pathToSave, TreeContext treeContext) throws IOException {
        LinkedHashMap<BitSet, Integer> sorted = ngramStatsBitset
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        List<Map.Entry<BitSet, Integer>> commonStats = new ArrayList<>(sorted.entrySet());
        Map<BitSet, Integer> editScriptToInd = new HashMap<>();
        for (int i = 0; i < commonStats.size(); i++) {
            BitSet editScript = commonStats.get(i).getKey();
            editScriptToInd.put(editScript, i);
        }

        File editScriptsFileUnmapped = new File(Paths.get(pathToSave).resolve("ngrams_ids.txt").toString());
        editScriptsFileUnmapped.getParentFile().mkdirs();
        editScriptsFileUnmapped.createNewFile();
        BufferedWriter editScriptsWriterUnmapped = new BufferedWriter(new FileWriter(editScriptsFileUnmapped.getAbsolutePath()));
        int num = 0;
        for (Map.Entry<BitSet, Integer> entry : sorted.entrySet()) {
            BitSet key = entry.getKey();
            String keyStr = encoder.decode(key);

            Integer value = entry.getValue();
            editScriptsWriterUnmapped.write(num + ") " +"[" + key.toString() + "] -> "+ keyStr + " = " + value + "\n");
            num++;
        }
        editScriptsWriterUnmapped.close();

        if (treeContext != null) {
            File editScriptsFileMapped = new File(Paths.get(pathToSave).resolve("ngrams_ids_with_tree_context.txt").toString());
            editScriptsFileMapped.getParentFile().mkdirs();
            editScriptsFileMapped.createNewFile();
            BufferedWriter editScriptsWriterMapped = new BufferedWriter(new FileWriter(editScriptsFileMapped.getAbsolutePath()));
            num = 0;
            for (Map.Entry<BitSet, Integer> entry : sorted.entrySet()) {
                BitSet key = entry.getKey();
                String keyStr = encoder.decode(key);
                keyStr = mapWithContext(keyStr, treeContext);

                Integer value = entry.getValue();
                editScriptsWriterMapped.write(num + ") " + keyStr + " = " + value + "\n");
                num++;
            }
            editScriptsWriterMapped.close();
        }


        int processed = 0;
        for (Map.Entry<String, Map<BitSet, Integer>> entry : fromChangeToHistBitset.entrySet()) {
            //System.out.println("Saving: " + processed + "/" + fromChangeToHistBitset.entrySet().size());
            processed++;

            String name = entry.getKey();
            //System.out.println("Saving hist: " + name );
            File histFile = new File(Paths.get(pathToSave).resolve(name).resolve("sparse_hist.txt").toString());

            histFile.getParentFile().mkdirs();
            histFile.createNewFile();
            BufferedWriter histWriter = new BufferedWriter(new FileWriter(histFile.getAbsolutePath()));

            Map<BitSet, Integer> hist = entry.getValue();

            List<Pair<Integer, Integer>> sparseHist = new ArrayList<>();

            for (Map.Entry<BitSet, Integer> histElem : hist.entrySet()) {
                Integer editScriptInd = editScriptToInd.getOrDefault(histElem.getKey(), -1);
                Integer height = histElem.getValue();
                sparseHist.add(new Pair<>(editScriptInd, height));
            }

            sparseHist.sort(Comparator.comparingInt(Pair::getFirst));

            for (Pair<Integer,Integer> pair : sparseHist) {
                histWriter.write(pair.getFirst() + " " + pair.getSecond() + "\n");
            }

            histWriter.close();
        }
    }


    private static String mapWithContext(String action, TreeContext treeContext) {
        String[] tokens = action.split(" ");
        int n = tokens.length;
        List<String> result = new ArrayList<>();
        result.add(tokens[0]);

        String nodeTypeTag = "@@";

        for (int i = 1; i < n; i++) {
            String token = tokens[i];

            if (token.endsWith(nodeTypeTag)) {
                String[] splitted = token.split(nodeTypeTag);
                if (isNumeric(splitted[0])) {
                    int type = Integer.parseInt(splitted[0]);
                    result.add(treeContext.getTypeLabel(type) + nodeTypeTag);
                }
            } else {
                result.add(token);
            }
        }

        return String.join(" ", result);
    }

    public static String actionToC(String action) {
        String[] tokens = action.split(" ");
        int n = tokens.length;
        List<String> result = new ArrayList<>();
        result.add(tokens[0]) ;
        result.add("(");
        String nodeTypeTag = "@@";

        for (int i = 1; i < n; i++) {
            String token = tokens[i];
            if (isNumeric(token)) {
                result.add(  token );
            }else{
                result.add("\"" +  token +"\"");
            }
            if(i < n-1){
                result.add(",");
            }
            
        }
        result.add(")");
        return String.join(" ", result);
    }

    public String NgramToText(BitSet key){
        return encoder.decode(key);
    }
}
