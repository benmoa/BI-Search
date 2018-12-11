package Model;

import java.io.*;

public class Processor {
    public ReadFile readFile; // pointer to ReadFile object
    public Parse parser; // pointer to Parse object
    public Indexer indexer; // pointer to Indexer object
    private boolean doStemming; // if to do stem
    // C'tor
    public Processor(ReadFile readFile,String mainPath, String savingPath, boolean doStemming) {
        this.readFile = readFile;
        parser = new Parse(this,mainPath, savingPath, doStemming);
        indexer = new Indexer(this, mainPath, savingPath);
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
                parser.ParseThisDoc(textOfDoc, (String)docName, readFile.allDocs_Map.get(docName).getFile_name());
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

}
