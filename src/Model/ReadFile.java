package Model;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReadFile {


    private boolean doStemming; //boolean -> do stemm\don't do stem
    private Processor processor; //the main object that connect all the stages in the preprocessing
    private static int numOfDocs; //count the number of docs
    private String mainPath; //the main path that we get from the user
    private String savingPath; //the path to save the posting and the vocabulary
    public Map<String,Document> allDocs_Map; //map of <docName, Doc>  Doc is an object with all the details of the document
    public Map<String, String> docsForIteration; //this map is for one iteration <docName, text>
    private int numOfFiles; //count the number of files, we iterate every 185 files
    private Set<String> languagesOfDocs; // a set of the languages in the docs
    private int totalNumOfFiles; // number of total docs in corpus


    //C'tor
    public ReadFile(Processor processor,String mainPath, String savingPath) {
        this.processor = processor;
        this.numOfDocs = 0;
        this.numOfFiles = 0;
        this.mainPath = mainPath;
        this.savingPath = savingPath;
        this.allDocs_Map = new HashMap<>();
    }
    //C'tor
    public ReadFile(Processor processor,String mainPath, String savingPath, boolean doStemming) {
        this.processor = processor;
        this.doStemming = doStemming;
        this.numOfDocs = 0;
        this.numOfFiles = 0;
        this.mainPath = mainPath;
        this.savingPath = savingPath;
        this.allDocs_Map = new HashMap<>();
        this.docsForIteration = new HashMap<>();
        this.languagesOfDocs = new HashSet<>();
    }
    // reads all corpus by 185 files in one iteration and sends it to parse
    public void readAllFiles () throws IOException, InterruptedException  {
        File mainPathDir = new File(mainPath + "\\corpus");
        String[] listOfDirs = mainPathDir.list();

        totalNumOfFiles = listOfDirs.length;
        //for each file add the docs to the map
        for (int i = 0; i < totalNumOfFiles; i++) {
            String currFilePath = mainPath + "\\corpus\\" + listOfDirs[i] + "\\" + listOfDirs[i];
            readOneFile_addToMap(currFilePath); //cut each doc in the file and make Document object from it
            numOfFiles++;

            //if we finished this iteration round OR this is the last file
            if((numOfFiles == 185) || (i + 1 == listOfDirs.length)){
                processor.parser.termFrequency = new HashMap<>();
                processor.Parse();
                numOfFiles = 0;
                docsForIteration = new HashMap<>(); //reset the docs for this iteration
                processor.parser.linesToPosting = new HashMap<>(); //reset the lines to posting of terms
                processor.parser.lineToPosting_Countries = new HashMap<>(); //reset the lines to posting of country
            }
        }

        listOfDirs = null;

        processor.mergePosting();
    }
    // split file to docs and taking from docs texts and countries
    public void readOneFile_addToMap(String currFilePath){
        try {

            FileInputStream input = new FileInputStream(new File(currFilePath));
            BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null){
                lineNumber++;
                if (line.contains("<DOC>")){
                    numOfDocs++;
                    Document doc = new Document();
                    doc.setFile_path(currFilePath);
                    doc.setStart_line(lineNumber);
                    line = br.readLine();
                    lineNumber++;
                    while (!(line.equals("</DOC>"))){
                        if (line.contains("<DOCNO>")){//searching the document number
                            doc.setDoc_number(removeTags(line));
                        }
                        //if (line.contains("<DATE1>") || line.contains("<DATE>")){//searching the date
                        //doc.setDate(removeTags(line));
                        //}
                        if (line.contains("<TI>")){//searching the title
                            doc.setTitle(removeTags(line));
                        }
                        if (line.contains("<F P=104>")){//country
                            String countryName = removeTags(line);
                            String[] names = countryName.split(" ");
                            countryName = names[0].toUpperCase();
                            doc.setCountry(countryName);
                        }
                        if (line.contains("<F P=105>")){//language
                            String lang = removeTags(line);
                            String[] langueges = lang.split(" ");
                            if(isWord(langueges[0]))
                                languagesOfDocs.add(langueges[0]);
                        }
                        if (line.contains("<TEXT>")) {//searching the <TEXT> tag
                            StringBuilder contentBuilder = new StringBuilder();
                            line = br.readLine();
                            while (!line.contains("</TEXT>")) { //add the lines until the end of the text
                                if (line.contains("<F P=104>")){//country (maybe inside the text area)
                                    String countryName = removeTags(line);
                                    String[] names = countryName.split(" ");
                                    countryName = names[0].toUpperCase();
                                    doc.setCountry(countryName);
                                }
                                if (line.contains("<F P=105>")){//language
                                    String lang = removeTags(line);
                                    String[] langueges = lang.split(" ");
                                    if(isWord(langueges[0]))
                                        languagesOfDocs.add(langueges[0]);
                                }

                                contentBuilder.append(line).append("\n");
                                line = br.readLine();
                            }
                            // add the <docName, textOfDoc> to this map
                            docsForIteration.put(doc.getDoc_number(),contentBuilder.toString());
                        }

                        line = br.readLine();
                        lineNumber++;
                    }
                    //this.allDocs_Set.add(doc);
                    allDocs_Map.put(doc.getDoc_number(),doc);
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    //if there is no numbers in the word
    private boolean isWord(String language) {
        for(int i = 0; i < language.length(); i++){
            if(Character.isDigit(language.charAt(i)) )
                return false;
        }
        return true;
    }
    // remove <[^>]*> from line
    private String removeTags(String line){
        line = line.replaceAll("<[^>]*>", "");
        line = line.trim();
        return line;
    }
    // getter
    public int getTotalNumOfFiles() {
        return totalNumOfFiles;
    }
    // getter
    public int getTotalNumOfDocs() {
        return numOfDocs;
    }
    // getter
    public Set<String> getLanguagesSet() {
        return languagesOfDocs;
    }
    //setter
    public void setLanguagesSet(Set<String> languagesOfDocs){this.languagesOfDocs = languagesOfDocs;}
    // getter
    public Processor getProcessor() {
        return processor;
    }

}