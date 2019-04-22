package Model;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class Searcher {

    private String query; //the string of the query
    public List<Map.Entry<String, Float>> resultList; //list of the results for the query
    private Processor processor; //pointer to the processor
    private Ranker ranker; //pointer to the ranker
    private Boolean doStemming; //check if stemming checked
    private Boolean doSemantic;//check if semantic checked
    private int numOfOriginalWords; //number of the original words in the query

    //C'tor
    public Searcher(String query,int numOfOriginalWords, Processor processor, Boolean doStemming, boolean doSemantic,Set<String> countries_Filter){
        this.processor = processor;
        this.doStemming = doStemming;
        this.doSemantic = doSemantic;
        this.numOfOriginalWords = numOfOriginalWords;
        this.query = query;
        this.resultList = new ArrayList<>();
            processor.parser.resetQueryMap();
            processor.parser.ParseThisDoc(query, "","",false);
            ranker = new Ranker(processor,this, processor.parser.getQueryTermsMap());

            //if there are filters of countries to do:
            if(countries_Filter.size() > 0) {
                removeFilteredCountries(countries_Filter);
                resultList = sortDocByRank(true);
            }else
                resultList = sortDocByRank(false);
    }
    //remove docs without the countries that we want
    private void removeFilteredCountries(Set<String> countries_Filter) {

            //taking the files for the current countries:
            Set<String> filesSet = getCountriesPlaces(countries_Filter);

            Iterator it = ranker.docsTotalRank.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String,Float> currDocInfo = (Map.Entry<String,Float>)it.next();
                if ((currDocInfo.getValue() != 0) && (filesSet.contains(currDocInfo.getKey()))) {
                    resultList.add(currDocInfo);
                }
            }

    }
    //this function take the set of countries to filter and return the docs of them
    private Set<String> getCountriesPlaces(Set<String> countries_filter) {
        Set<String> allFilesToTerm = new HashSet<>();

        //for each country from the filter:
        for (String countryName : countries_filter) {
            CountryInfo currCountryInfo = processor.indexer.countryMap.get(countryName);

            TermInfo currTermInfo = null;
            if (processor.parser.allTerms_Map.containsKey(countryName))
                currTermInfo = processor.parser.allTerms_Map.get(countryName);
            else if (processor.parser.allTerms_Map.containsKey(countryName.toLowerCase()))
                currTermInfo = processor.parser.allTerms_Map.get(countryName.toLowerCase());
            else if (processor.parser.allTerms_Map.containsKey(countryName.toUpperCase()))
                currTermInfo = processor.parser.allTerms_Map.get(countryName.toUpperCase());


            String termLine = "";
            String countryLine = "";
            char firstChar = countryName.charAt(0);
            File postingFile;
            String postingPath = processor.parser.savingPath;
            //checking for the path
            if (!postingPath.contains("stemme")) {
                if (processor.isDoStemming()) // if checkbox of stem is selected
                    postingPath += "\\with stemme";
                else
                    postingPath += "\\without stemme";
            }

            try{
                //if there is term like this in the All terms map
                if (currTermInfo != null) {
                    int lineInPosting = currTermInfo.getLineInPosting();

                    postingFile = new File(postingPath + "\\PostingForTerms\\" + Character.toLowerCase(firstChar) + ".txt");
                    RandomAccessFile randomAccessFile = new RandomAccessFile(postingFile, "r");

                    randomAccessFile.seek(0);
                    randomAccessFile.seek(lineInPosting);
                    byte[] termLineArr = new byte[currTermInfo.getLengthInFile()];
                    randomAccessFile.read(termLineArr);
                    termLine = new String(termLineArr);

                    termLine = termLine.substring(termLine.indexOf(":") + 2);
                    allFilesToTerm.addAll(getFilesFromLine(termLine));
                }


                //if there is term like this in the Country map
                if (currCountryInfo != null) {
                    int lineInPosting = currCountryInfo.getLineInPosting();

                    postingFile = new File(postingPath + "\\PostingForCountries\\" + Character.toUpperCase(firstChar) + ".txt");

                    RandomAccessFile randomAccessFile = new RandomAccessFile(postingFile, "r");

                    randomAccessFile.seek(0);
                    randomAccessFile.seek(lineInPosting);
                    byte[] termLineArr = new byte[currCountryInfo.getLengthInFile()];
                    randomAccessFile.read(termLineArr);
                    countryLine = new String(termLineArr);

                    countryLine = countryLine.substring(countryLine.indexOf(":") + 2);
                    allFilesToTerm.addAll(getFilesFromLine(countryLine));
                }
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return allFilesToTerm;
    }
    //taking the docs from the given line
    private Set<String> getFilesFromLine(String line) {
        String[] docsInfoArr = line.split(",");
        Set<String> files = new HashSet<>();
        for (String docInfo:docsInfoArr) {
            String[] curr = docInfo.split(" ");
            files.add(curr[0]);
        }
        return files;
    }
    //sort document by rate of cossim and BM25
    private List<Map.Entry<String, Float>> sortDocByRank(boolean isFiltered) {
        Iterator it;

        //if there was filters of countries:
        if(isFiltered) {
            Collections.sort(resultList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            it = resultList.iterator();
        }else {//if it's without filter:
            List<Map.Entry<String, Float>> list = new ArrayList<>(ranker.docsTotalRank.entrySet());
            Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            it = list.iterator();
        }
        List<Map.Entry<String,Float>> tmp = new ArrayList<>();

        //taking the best 50 relevant docs:
        while (it.hasNext() && tmp.size() < 50) {
            Map.Entry<String,Float> o = (Map.Entry<String,Float>)it.next();
            if (o.getValue() != 0 && tmp.size() < 50) {
                tmp.add(o);
            }
        }
        return tmp;
    }
    //getter
    public Boolean getDoSemantic() {
        return doSemantic;
    }
    //getter
    public int getNumOfOriginalWords() {
        return numOfOriginalWords;
    }
    //getter
    public String getQuery() {
        return query;
    }
}
