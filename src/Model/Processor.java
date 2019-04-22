package Model;

import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Processor {
    public ReadFile readFile; // pointer to ReadFile object
    public Parse parser; // pointer to Parse object
    public Indexer indexer; // pointer to Indexer object
    private boolean doStemming; // if to do stem
    // holds pairs of top 5 terms in doc, each terms with his name and his tf
    private List<Pair<String,Float>> top5terms;

    // C'tor
    public Processor(String mainPath, String savingPath, boolean doStemming, boolean isQuery) {
        this.readFile = new ReadFile(this,mainPath,savingPath);
        this.parser = new Parse(this,mainPath,savingPath, doStemming, isQuery);
        this.indexer = new Indexer(this, mainPath);
        this.doStemming = doStemming;
        this.top5terms = new ArrayList<>();
    }
    // C'tor
    public Processor(String mainPath, String savingPath, boolean doStemming) {
        this.readFile = new ReadFile(this,mainPath,savingPath,doStemming);
        this.parser = new Parse(this,mainPath, savingPath, doStemming);
        this.indexer = new Indexer(this, mainPath, savingPath);
        this.doStemming = doStemming;
    }
    // sends docs to parse and than to index
    public void Parse() {

        //long startTime=System.currentTimeMillis();

        //for each document - do parse and stemm
        //Set<String> keySet = readFile.docsForIteration.keySet();
        Object[] keySetArr = readFile.docsForIteration.keySet().toArray();
        for (Object docName : keySetArr)
        {
            String textOfDoc = readFile.docsForIteration.get((String)docName);
            //if there is something in the text area, do parse
            if(textOfDoc != "") {
                parser.ParseThisDoc(textOfDoc, (String)docName, readFile.allDocs_Map.get(docName).getFile_name(),false);
            }
        }



        //----------------
        //--terms Index---
        //----------------
        //send to the indexer the term freq map, the lines to write in posting, the number of docs in this iteration
        try {
            indexer.addFileToTermIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //--------------------
        //---countries index--
        //--------------------
        try {
            if(parser.lineToPosting_Countries.size() != 0)
                indexer.addFileToCountriesIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
    // sends to merge posting in indexer
    public void mergePosting() throws IOException {
        //merge TERMS
        indexer.mergePosting("Terms", indexer.tempPostingsForTerms);

        //merge COUNTIES
        indexer.mergePosting("Countries", indexer.tempPostingsForCountry);
    }

    public void readAllFiles() throws IOException, InterruptedException {
        readFile.readAllFiles();
    }




    //----------------
    //----Part B------
    //----------------

    //return five Most important terms in map (only capital letters)
    public void ParseAndFindFive(String docName) {
        Document docInfo = readFile.allDocs_Map.get(docName);
        String fileName = docInfo.getFile_name();
        try {
            File file = new File(indexer.mainPath + "//corpus//" + fileName + "//" + fileName);
            FileInputStream inp = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(inp, StandardCharsets.UTF_8));
            StringBuilder allText = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) { // loop until end of file
                if (line.contains(docName)) { // check if we got to doc name that we looked for
                    while (!(br.readLine()).contains("<TEXT>")); // looking for text tag
                    line = br.readLine();
                    allText.append(line).append("\n");
                    while (((line = br.readLine()) != null) && (!line.contains("</TEXT>"))) { // looking for ending text
                        allText.append(line).append("\n");
                    }
                    break;
                }
            }
            parser.setIsQuery(true);
            parser.resetQueryMap();
            parser.ParseThisDoc(allText.toString(),docName,fileName,false);
            // init list pairs to saving results
            List<Pair<String,Integer>> top5TermsInDoc = new ArrayList<>();
            Map <String, Integer> docTerms = parser.getQueryTermsMap();
            Iterator it = docTerms.keySet().iterator();
            // remove all terms with lower case
            while (it.hasNext()) {
                String curr = (String) it.next();
                if (parser.isAllCapitalInString(curr)){
                    top5TermsInDoc.add(new Pair<>(curr,docTerms.get(curr)));
                }
            }

            Collections.sort(top5TermsInDoc, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            top5TermsInDoc = top5TermsInDoc.subList(0,5);
            //top5terms = top5TermsInDoc;

            //normalize the list: (tf/max(tf))
            int maxTF = top5TermsInDoc.get(0).getValue();
            it = top5TermsInDoc.iterator();
            int i = 0;
            top5terms = new ArrayList<>();
            while (it.hasNext()) {
                Pair<String,Integer> currItem = (Pair<String,Integer>)it.next();
                float normalizedVal = (float)currItem.getValue() / maxTF;
                String term = currItem.getKey();
                top5terms.add(i,new Pair<String, Float>(term,normalizedVal));
                i++;
            }

        } catch (IOException o) {

        }
    }
    // getter
    public List<Pair<String, Float>> getTop5terms() {
        return top5terms;
    }
    public boolean isDoStemming() {
        return doStemming;
    }

}
