public static void LaseLookLike(Path pathToDataset1, Path pathToListFile1, Path pathToDataset2, Path pathToListFile2,
    Path pathToMatrix, String version, String verbose) throws IOException {

        // check directory structure
        File directory = new File(pathToMatrix.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }


        File directory2 = new File(pathToMatrix.toString() +"\\ast");
        if (!directory2.exists()) {
            directory2.mkdir();
        }
        // first dataset - test (bad files only)
        List<String> defects1 = Files.readAllLines(pathToListFile1);
        List<String> defectFiles1 = new ArrayList<String>();

        String badFolderName1 = pathToDataset1.toString() + "\\bad";
//        String goodFolderName1 = pathToDataset1.toString() + "\\good";

        try {

            List<String> result1 = Files.walk(Paths.get(badFolderName1)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result1) {
                boolean useFile = false;

                for (String defect : defects1) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles1.add(fName);
                }
            }

            // second dataset - template ( bad + good files)
            List<String> defects2 = Files.readAllLines(pathToListFile2);

            List<String> defectFiles2 = new ArrayList<String>();

            String badFolderName2 = pathToDataset2.toString() + "\\bad";
            String goodFolderName2 = pathToDataset2.toString() + "\\good";
            String astFolderName = pathToMatrix.toString() + "\\ast";

            List<String> result2 = Files.walk(Paths.get(badFolderName2)).filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            // collect all files for defect to build matrix
            for (String fName : result2) {
                boolean useFile = false;

                for (String defect : defects2) {

                    if (fName.contains("\\" + defect + "\\")) {
                        var Code = Files.readString(Paths.get(fName));
                        if (Code.length() <= MAX_FILE_SIZE)
                            useFile = true;
                        break;
                    }
                }
                if (useFile) {
                    defectFiles2.add(fName);
                }
            }

        // defect files is a collection of bad files
        if (defectFiles1.size() > 0 && defectFiles2.size() > 0) {

            // collect common actions for cluster here
            double[][] weightMatrix = new double[defectFiles2.size()][defectFiles1.size()];
            int[] sizes = new int[defectFiles2.size()];

            ASTGenerator generator = null;

            if (version.toLowerCase().equals("abstract")) {
                generator = new CachedASTGenerator(new NamesASTNormalizer());
            } else {
                generator = new CachedASTGenerator(new BasicASTNormalizer());
            }

            for (int i = 0; i < defectFiles2.size(); i++) {

                // get template defects from second dataset
                String defectB = defectFiles2.get(i);
                String defectB_Name="";

                for (String defect : defects2) {

                    if (defectB.contains("\\" + defect + "\\")) {
                        defectB_Name =defect;
                        break;
                    }
                }

                TreeContext dstB = null;
                List<Action> actB = null;
                List<String> seekCode= new ArrayList<String>();
                String emuCode="";

                
                File actionsFile = new File( astFolderName +  "//" + defectB_Name +".ES");
                File seekFile = new File(astFolderName +  "//" + defectB_Name +".seek");
                
                

                try {
                    var fromCode = Files.readString(Paths.get(defectB));
                    var toCode = Files.readString(Paths.get(defectB.replace(badFolderName2, goodFolderName2)));
                    if (fromCode.length() <= MAX_FILE_SIZE && toCode.length() <= MAX_FILE_SIZE) {

                        var fromSolution = new Solution(fromCode, "B_BAD", "B_BAD", FAIL);
                        var toSolution = new Solution(toCode, "B_GOOD", "B_GOOD", OK);

                        TreeContext srcB = null;

                        srcB = generator.buildTreeContext(fromSolution);
                        dstB = generator.buildTreeContext(toSolution);

                        Matcher matcherAst = Matchers.getInstance().getMatcher(srcB.getRoot(), dstB.getRoot());
                        System.out.println("Compare trees");
                        try {
                            matcherAst.match();
                        } catch (NullPointerException e) {
                            System.out.println(e.getMessage());
                        }

                        ActionGenerator actionGenerator = new ActionGenerator(srcB.getRoot(), dstB.getRoot(),
                                matcherAst.getMappings());
                        try {
                            actionGenerator.generate();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        }

                        actB = actionGenerator.getActions();



                        if (actB != null && actB.size() > 0) {
                            seekCode.clear();
                            emuCode = "";

                      
                            for (Action action : actB) {
                                
                                ITree actNode= null;
                                String actString = action.getName();
                                String seekString = "";
                                

                                if(action.getName()=="UPD"){
                                    Update u = (Update) action;
                                    actNode = u.getNode();
                                    actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                    if (actNode.hasLabel())
                                        seekString+=ActionContext.GetContextPath(action, false, srcB);
    
                                    actString += " change to " + u.getValue();
                                }

                                if(action.getName()=="MOV" || action.getName()=="INS" ){
                                    Addition ad = (Addition) action;
                                    actNode = ad.getParent();
                                    actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                    if (actNode.hasLabel())
                                        seekString+=ActionContext.GetContextPath(action, false, srcB);
                  
                                    
                                }

                                if(action.getName()=="DEL"){
                                    Delete d = (Delete) action;
                                    actNode = d.getNode();
                                    actString += " " + ActionContext.GetContextPath(action, false, srcB);
                                    if (actNode.hasLabel())
                                        seekString+=ActionContext.GetContextPath(action, false, srcB);
              
                                }

                                if(verbose.equals("yes")){
                                    if (seekString.length()>0){
                                        ITree nfa = ActionContext.GetContextRoot(action);

                                        if(nfa != null && nfa.getLength() >0){
                                            actString +="\r\n----------- Code from template source -----------------------------\r\n";
                                            actString +=fromCode.substring( nfa.getPos(), (fromCode.length()<nfa.getEndPos()?fromCode.length():nfa.getEndPos()) );    
                                            actString +="\r\n-------------------------------------------------------------------\r\n";
                                        }

                                        if(nfa != null){
                                            nfa = matcherAst.getMappings().getSrc(nfa);
                                            if(nfa != null && nfa.getLength() >0){
                                                actString +="\r\n----------- Code from template destination--------------------------\r\n";
                                                actString +=toCode.substring( nfa.getPos(), (toCode.length()<nfa.getEndPos()?toCode.length():nfa.getEndPos()) );    
                                                actString +="\r\n-------------------------------------------------------------------\r\n";
                                            }
                                        }
                                    }

                                }

                                emuCode += actString + "\n";
                                if(seekString.length()>0 ){
                                    if(! seekCode.contains(seekString))
                                        seekCode.add(seekString);
                                }
                                    

                            }

                            BufferedWriter writer= null;

                            sizes[i]=seekCode.size();

                            if(verbose.equals("yes")){
                                if (seekCode.size() >= 5) { 
                                    writer = null;
                                    writer = new BufferedWriter(new FileWriter(actionsFile.getAbsolutePath()));
                                    writer.write(emuCode);
                                    writer.close();
                                

                                    writer = null;
                                    writer = new BufferedWriter(new FileWriter(seekFile.getAbsolutePath()));
                                    for(String sc: seekCode){
                                        writer.write(sc +"\r\n");
                                    }
                                    writer.close();
                                    writer = null;
                                }
                            }

                        }
                        fromSolution = null;
                        toSolution = null;
                    }
                } catch (Exception any) {
                    System.out.println(any.getMessage());
                    any.printStackTrace();
                }


            if (seekCode.size() >= 5) {    

                // scan all dataset 1 for test with template item from dataset 2
                for (int j = 0; j < defectFiles1.size(); j++) {
                    System.out.println(">>>>" + i + "(" + defectFiles2.size() + ")" + " x " + j + "("
                            + defectFiles1.size() + ")");


                    weightMatrix[i][j] = 0;
                    
                    String defectA = defectFiles1.get(j);
                    TreeContext srcA = null;

                    try {
                        srcA = null;
                        var fromCodeA = Files.readString(Paths.get(defectA));
                        if (fromCodeA.length() <= MAX_FILE_SIZE) {
                            var fromSolutionA = new Solution(fromCodeA, "A_BAD", "A_BAD", FAIL);
                            srcA = generator.buildTreeContext(fromSolutionA);
                            fromSolutionA = null;
                        }

                        if (srcA != null && dstB != null && actB != null) {
                            List<String> seekCheck = new ArrayList<String>();

                            if (seekCode.size() >= 5) {
                               
                                List<ITree> po = TreeUtils.preOrder( srcA.getRoot());
                                for(ITree n: po){
                                    String c = ActionContext.GetNodePath(n, true, srcA)  ;
                                    if(c.length()>0){
                                        if (seekCode.contains(c) ){
                                            if(!seekCheck.contains(c))
                                                seekCheck.add(c);
                                        }
                                            
                                    }
                                    // if all strings are found we can stop checking
                                    if(seekCheck.size() == seekCode.size())
                                        break;
                                }
                                
                                weightMatrix[i][j] = 100.0 *  seekCheck.size() / seekCode.size();
                            }
                            seekCheck.clear();
                            seekCheck=null;
                        }

                    } catch (Exception any) {
                        System.out.println(any.getMessage());
                        any.printStackTrace();
                    }

                    
                }

                // write csv result for given (each) defectB
                {

                    int Size = 0, Cnt = 0;
                    String calcDefect = "";
                    for (int j = 0; j < defectFiles1.size(); j++) {
                        if (weightMatrix[i][j] == 100.0)
                            Cnt++;
                        if (weightMatrix[i][j]> 0.0)
                            Size++;
                        //Size += weightMatrix[i][j];
                    }

                    for (String defect : defects2) {
                        if (defectFiles2.get(i).contains("\\" + defect + "\\")) {

                            calcDefect = defect;
                            break;
                        }
                    }

                    String matrixFile = pathToMatrix.toString() + "\\";
                    //matrixFile += "(" + calcDefect + ")_" +Cnt + "_" + Size + "_seek_"+ sizes[i]  + ".csv";
                    matrixFile +=  calcDefect + "_F." +Cnt + "_P."+ Size + "_S."+ sizes[i]  + ".csv";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));
                    writer.write("\"" + calcDefect + "\"," + Cnt + "," + Size + "\r\n");
                    {

                        writer.write("\"base\"");
                        for (int j = 0; j < defectFiles1.size(); j++) {
                            for (String defect : defects1) {
                                if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                                    writer.write(",\"" + defect + "\"");
                                    break;
                                }
                            }
                        }
                        writer.write("\r\n\"" + calcDefect + "\"");
                        for (int j = 0; j < defectFiles1.size(); j++) {
                            writer.write("," + weightMatrix[i][j]);
                        }
                        writer.write("\r\n");
                    }
                    writer.close();
                }

            }

        }

        String matrixFile = pathToMatrix.toString() + "\\looklike.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(matrixFile));

        // first row - list of defects to test
        writer.write("\"template to defect\"");
        for (int j = 0; j < defectFiles1.size(); j++) {

            for (String defect : defects1) {
                if (defectFiles1.get(j).contains("\\" + defect + "\\")) {
                    writer.write(",\"" + defect + "\"");
                    break;
                }
            }
        }
        writer.write("\r\n");

        // other rows one per template defect
        for (int i = 0; i < defectFiles2.size(); i++) {

            for (String defect : defects2) {
                if (defectFiles2.get(i).contains("\\" + defect + "\\")) {
                    writer.write("\"" + defect + "\"");
                    break;
                }
            }
            for (int j = 0; j < defectFiles1.size(); j++) {
                writer.write("," + weightMatrix[i][j]);
            }
            writer.write("\r\n");
        }
        writer.close();

    }

} catch (IOException e) {
    e.printStackTrace();
}
}
