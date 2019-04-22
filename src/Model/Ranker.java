package Model;

import java.io.*;
import java.util.*;

public class Ranker {

    private Processor processor; //pointer to the processor
    private Searcher searcher;//pointer to the searcher
    private double avgLengthOfDoc; //holding the avarage length of the docs in the current cprpus
    public Map<String, Float> docsTotalRank; // each doc and his own rank. calc by both bm25 and cossim formula
    private Map<String, Integer> queryTermsMap; //having the terms from the query <termName,TF>
    private Set<String> originalWords;//set of the original words of the query

    //original maps and fields:
    private Map<String,Float> docsTotal_tfidf;//foreach Doc ,tf*idf foreach term in query(Sum Wi,j * Wiq)
    private Map<String, Float> docsCosSim;// <docName,cosSim> foreach Document, Cos Similarity
    private Map<String, Float> docs_BM25; // calc bm25 value for each doc
    private float maxVal_BM25;//max value of BM25 for normalizing
    private float maxVal_CosSim;//max value of CosSim for normalizing
    private Set<String> inTenPercentOfDoc_Set; //Set that says if the term is in the 10% first words of doc

    //maps and fields for the Semantic and Description:
    private Map<String,Float> docsTotal_tfidf_Extra;//foreach Doc ,tf*idf foreach term in query(Sum Wi,j * Wiq)
    private Map<String, Float> docsCosSim_Extra;// <docName,cosSim> foreach Document, Cos Similarity
    private Map<String, Float> docs_BM25_Extra; // calc bm25 value for each doc
    private float maxVal_BM25_Extra;//max value of BM25 of the extra words for normalizing
    private float maxVal_CosSim_Extra;//max value of CosSim of the extra words for normalizing


    //C'tor
    public Ranker(Processor processor,Searcher searcher,Map<String, Integer> queryTermsMap){
        this.docsTotal_tfidf = new HashMap<>();
        this.docsCosSim = new HashMap<>();
        this.processor = processor;
        this.searcher = searcher;
        this.queryTermsMap = queryTermsMap;
        this.docs_BM25 = new HashMap<>();
        this.docsTotalRank = new HashMap<>();
        this.avgLengthOfDoc = 0;

        //if there is semantic or description:
        docsCosSim_Extra = new HashMap<>();
        docs_BM25_Extra = new HashMap<>();
        docsTotal_tfidf_Extra = new HashMap<>();
        inTenPercentOfDoc_Set = new HashSet<>();
        if(searcher.getDoSemantic()) {
            fillOriginalWords();
        }

        calcAvgLenOfDoc();

        findDocsForTerm();

        calculateCosSim();

        calcTotalRank();

        //reset the maps:
        this.docs_BM25 = null;
        this.docsCosSim = null;
        this.docsTotal_tfidf = null;
        this.queryTermsMap = null;
        this.inTenPercentOfDoc_Set = null;
    }
    //this function fill the set of the original words in the query
    private void fillOriginalWords() {
        originalWords = new HashSet<>();
        int numOfOriginalWords = searcher.getNumOfOriginalWords();
        String[] splittedQuery = (searcher.getQuery()).split(" ");
        for (int i = 0;i < numOfOriginalWords; i++)
            originalWords.add((splittedQuery[i]).toLowerCase());
    }
    //this function taking each term and finding the docs of it from the posting files
    private void findDocsForTerm() {

        //running on all the words in the query
        for (String queryWord : queryTermsMap.keySet()) {
            try {
                TermInfo currTermInfo = null;
                if (processor.parser.allTerms_Map.containsKey(queryWord))
                    currTermInfo = processor.parser.allTerms_Map.get(queryWord);
                else if (processor.parser.allTerms_Map.containsKey(queryWord.toLowerCase()))
                    currTermInfo = processor.parser.allTerms_Map.get(queryWord.toLowerCase());
                else if (processor.parser.allTerms_Map.containsKey(queryWord.toUpperCase()))
                    currTermInfo = processor.parser.allTerms_Map.get(queryWord.toUpperCase());

                //if there is term like this in the All terms map
                if (currTermInfo != null) {
                    int lineInPosting = currTermInfo.getLineInPosting();

                    //if (lineInPosting == -1) {
                    //    lineInPosting = processor.indexer.cache.get(queryWord).lineInPosting;
                    //}

                    File postingFile;
                    String postingPath = processor.parser.savingPath;
                    if (!postingPath.contains("stemme")) {
                        if (processor.isDoStemming()) // if checkbox of stem is selected
                            postingPath += "\\with stemme";
                        else
                            postingPath += "\\without stemme";
                    }

                    char firstChar = queryWord.charAt(0);
                    firstChar = Character.toLowerCase(firstChar);
                    if (firstChar < 'a' || firstChar > 'z') //if it's not a letter
                        postingFile = new File(postingPath + "\\PostingForTerms\\numbers.txt");
                    else//open the letter file
                        postingFile = new File(postingPath + "\\PostingForTerms\\" + firstChar + ".txt");


                    RandomAccessFile randomAccessFile = new RandomAccessFile(postingFile, "r");

                    randomAccessFile.seek(0);
                    randomAccessFile.seek(lineInPosting);
                    byte[] termLineArr = new byte[currTermInfo.getLengthInFile()];
                    randomAccessFile.read(termLineArr);


                    addWToTerm(new String(termLineArr), currTermInfo);
                    calcBm25(new String(termLineArr), currTermInfo);
                    calcTopTenInDoc(new String(termLineArr), currTermInfo);
                }
            }catch (Exception e){}
        }


    }
    //this function gives the term extra rank if it's in the top 10% words in the doc
    private void calcTopTenInDoc(String line, TermInfo currTermInfo) {
        line = line.substring(line.indexOf(":") + 2);
        String[] docsForTermArr = line.split(",");
        int i = docsForTermArr.length;

        try {
            //running on the docs of the term
            for (; i < docsForTermArr.length; i++) {
                String[] infoForCurrDoc = docsForTermArr[i].split(" ");
                String docName = infoForCurrDoc[0];
                String stringOfplacesInDoc = infoForCurrDoc[2];
                int lengthOfDoc = processor.readFile.allDocs_Map.get(docName).getNumOfTerms();
                String[] placesArr = stringOfplacesInDoc.split("!");
                int firstAppearance = Integer.parseInt(placesArr[0]);
                float appearPercent = (float)((float)firstAppearance / lengthOfDoc);
                if (appearPercent <= 0.1)
                    inTenPercentOfDoc_Set.add(docName);
            }
        }catch (Exception e){

        }
    }
    //this function calculate the BM25
    private void calcBm25(String line, TermInfo currTermInfo) {
        line = line.substring(line.indexOf(":") + 2);
        String[] docsForTermArr = line.split(",");

        for(int i = 0; i < docsForTermArr.length; i++) {
            try {
                String[] infoForCurrDoc = docsForTermArr[i].split(" ");
                String docName = infoForCurrDoc[0];
                int freqTermInDoc = Integer.parseInt(infoForCurrDoc[1]);
                float idf = currTermInfo.getIdf();
                int lengthOfDoc = processor.readFile.allDocs_Map.get(docName).getNumOfTerms();
                // calculating BM25 formula
                double k = 1.2;
                double b = 0.75;
                float numerator = (float) ((float) freqTermInDoc * (k + 1));
                float denominator = (float) ((float) (freqTermInDoc) + (k * ((1 - b) + b * (lengthOfDoc / avgLengthOfDoc))));
                float ans = (float) ((float) idf * (numerator / denominator));

                addTo_BM25Map(currTermInfo.getTerm(), docName, ans);
            }catch (Exception e){}
        }
    }
    //add Wi,j term
    private void addWToTerm(String line, TermInfo currTermInfo) {
        String termName = currTermInfo.getTerm();
        line = line.substring(line.indexOf(":") + 2);
        String[] docsForTermArr = line.split(",");

        for (int i = 0; i < docsForTermArr.length; i++) {
            try {
                String[] infoForCurrDoc = docsForTermArr[i].split(" ");
                String docName = infoForCurrDoc[0];
                int freqTermInDoc = Integer.parseInt(infoForCurrDoc[1]);
                int maxTF = processor.readFile.allDocs_Map.get(docName).getMax_tf();
                //int lengthOfDoc = processor.readFile.allDocs_Map.get(docName).getNumOfTerms();
                float freqDIVIDEmax = (float) ((float) freqTermInDoc / maxTF);             // tf = f/max(f)
                float tfIdf = (float) ((float) freqDIVIDEmax * currTermInfo.getIdf()); // tf*idf = f/max(f) * idf

                addToTotal_tfIdfMap(termName, docName, tfIdf);
            }catch (Exception e){}
        }
    }
    //calculate cosSim for each document into docsSumW
    private void calculateCosSim() {
        double sumWiq2 = queryTermsMap.keySet().size();//number of words in the query

        for (String docName: docsTotal_tfidf.keySet()) {
            float sumWijWiq = docsTotal_tfidf.get(docName);
            double sumWij2 = processor.readFile.allDocs_Map.get(docName).getSumWeights();
            float cosSim = (float)((float)sumWijWiq / (Math.sqrt(sumWij2 * sumWiq2)));
            docsCosSim.put(docName,cosSim);
        }

        for (String docName: docsTotal_tfidf_Extra.keySet()) {
            float sumWijWiq = docsTotal_tfidf_Extra.get(docName);
            double sumWij2 = processor.readFile.allDocs_Map.get(docName).getSumWeights();
            float cosSim = (float)((float)sumWijWiq / (Math.sqrt(sumWij2 * sumWiq2)));
            docsCosSim_Extra.put(docName,cosSim);
        }
    }
    // this function will calc total rank by using formula : 0.2 * cossim + 0.8 * BM25
    private void calcTotalRank() {
        maxVal_BM25 = 0;

        //checking for the maximum value of the BM25 and CosSim
        for (String docName : docs_BM25.keySet()) {
            float bm25 = docs_BM25.get(docName);
            float cosSim = docsCosSim.get(docName);
            if(bm25 > maxVal_BM25)
                maxVal_BM25 = bm25;
            if(cosSim > maxVal_CosSim)
                maxVal_CosSim = cosSim;
        }

        //creating the final Weights for docs:
        for (String docName : docsCosSim.keySet()) {
            float bm25 = docs_BM25.get(docName);
            bm25 = (float)((float)bm25 / maxVal_BM25); //normalize BM25
            float cossim = docsCosSim.get(docName);
            cossim = (float)((float)cossim / maxVal_CosSim); //normalize CosSim
            float half_bm25 = (float)((0.8) * (float)(bm25));
            float half_cosSim = (float)((0.2) * (float)(cossim));
            float totalValue =  half_bm25 + half_cosSim;
            //float totalValue = bm25;
            //float totalValue = cossim;

            docsTotalRank.put(docName, totalValue);
        }

        //if there EXTRA:
        if(searcher.getDoSemantic()) {
            maxVal_BM25_Extra = 0;

            //checking for the maximum value of the BM25 and CosSim
            for (String docName : docs_BM25_Extra.keySet()) {
                float bm25 = docs_BM25_Extra.get(docName);
                float cosSim = docsCosSim_Extra.get(docName);
                if (bm25 > maxVal_BM25_Extra)
                    maxVal_BM25_Extra = bm25;
                if (cosSim > maxVal_CosSim_Extra)
                    maxVal_CosSim_Extra = cosSim;
            }

            //creating the final Weights for docs:
            for (String docName : docsCosSim_Extra.keySet()) {
                float bm25 = docs_BM25_Extra.get(docName);
                bm25 = (float) ((float) bm25 / maxVal_BM25_Extra); //normalize BM25
                float cossim = docsCosSim_Extra.get(docName);
                cossim = (float) ((float) cossim / maxVal_CosSim_Extra); //normalize CosSim
                float half_bm25 = (float) ((0.8) * (float) (bm25));
                float half_cosSim = (float) ((0.2) * (float) (cossim));
                float totalValue = half_bm25 + half_cosSim;
                //float totalValue = bm25;
                //float totalValue = cossim;
                float weight = (float)0.55;
                //insert the EXTRA word with HALF rank to the map
                if (docsTotalRank.containsKey(docName)) { // if docname already exists in map
                    float rank = docsTotalRank.remove(docName);
                    docsTotalRank.put(docName,rank + (totalValue * weight));
                }
                else { // docname doesn't exists on map
                    docsTotalRank.put(docName, (totalValue * weight));
                }
            }

            checkForTenPercent();
        }
    }
    //check if we need to add extra rank for some docs because the 10% words
    private void checkForTenPercent() {
        Iterator it = inTenPercentOfDoc_Set.iterator();
        //running on the docs that have the word in the 10 %
        while (it.hasNext()){
            try {
                String docName = (String) it.next();
                float rank = docsTotalRank.remove(docName);
                float newRank = rank * (float) 1.2;
                docsTotalRank.put(docName, newRank);
            }catch (Exception e){}
        }
    }
    // this function will calculate the average size of doc in all corpus
    private void calcAvgLenOfDoc () {
        Iterator it = processor.readFile.allDocs_Map.keySet().iterator();
        int counter = 0;
        while (it.hasNext()) {
            String curr = (String)it.next();
            int numOfTerms = processor.readFile.allDocs_Map.get(curr).getNumOfTerms();
            avgLengthOfDoc += numOfTerms;
            counter ++;
        }
        avgLengthOfDoc = ((avgLengthOfDoc) / (counter));
    }

    //this function take the tf*idf of the current doc and add it to the total tf*idf of the doc
    private void addToTotal_tfIdfMap(String termName,String docName, float tfIdf) {
        //if its not semantic:
        if(!searcher.getDoSemantic()) {
            if (docsTotal_tfidf.containsKey(docName)) {
                docsTotal_tfidf.replace(docName, docsTotal_tfidf.get(docName) + tfIdf);
            } else {
                docsTotal_tfidf.put(docName, tfIdf);
            }
        }else {//it's semantic or description
            //if its original word in the query:
            if(originalWords.contains(termName.toLowerCase())){
                if (docsTotal_tfidf.containsKey(docName)) {
                    docsTotal_tfidf.replace(docName, docsTotal_tfidf.get(docName) + tfIdf);
                } else {
                    docsTotal_tfidf.put(docName, tfIdf);
                }
            }else {//it's an EXTRA word
                if (docsTotal_tfidf_Extra.containsKey(docName)) {
                    docsTotal_tfidf_Extra.replace(docName, docsTotal_tfidf_Extra.get(docName) + tfIdf);
                } else {
                    docsTotal_tfidf_Extra.put(docName, tfIdf);
                }
            }
        }
    }


    //this function add the rank of the BM25 to the right map
    private void addTo_BM25Map(String termName, String docName, float ans) {
        //if its not semantic:
        if(!searcher.getDoSemantic()) {
            if(docs_BM25.containsKey(docName)) { // if docname already has value in map
                docs_BM25.replace(docName, (float)(docs_BM25.get(docName) + ans));
            }
            else{ // if docname doesn't exist in map
                docs_BM25.put(docName,ans);
            }
        }else {//it's semantic or description
            //if its original word in the query:
            if(originalWords.contains(termName.toLowerCase())){
                if(docs_BM25.containsKey(docName)) { // if docname already has value in map
                    docs_BM25.replace(docName, (float)(docs_BM25.get(docName) + ans));
                }
                else{ // if docname doesn't exist in map
                    docs_BM25.put(docName,ans);
                }
            }else {//it's an EXTRA word
                if(docs_BM25_Extra.containsKey(docName)) { // if docname already has value in map
                    docs_BM25_Extra.replace(docName, (float)(docs_BM25_Extra.get(docName) + ans));
                }
                else{ // if docname doesn't exist in map
                    docs_BM25_Extra.put(docName,ans);
                }
            }
        }
    }

}