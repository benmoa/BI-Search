package Model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StopWords {

    private Set<String> stopWords_Set; // Set that holds all stop words

    //C'tor
    public StopWords(String mainPath) {
        stopWords_Set = new HashSet<>();
        //cutting the path and adding the file name
        String stopWords_path = mainPath + "\\stop_words.txt";
        try {
            fillSet(stopWords_path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //filling the set of the stop words, taking them from the file
    private void fillSet(String stopWords_path) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(stopWords_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line;

        while ((line = br.readLine()) != null) {
            if((!line.equals("Between")) && (!line.equals("between")) && (!line.equals("BETWEEN"))
                    && (!line.equals("MAY")) && (!line.equals("May")) && (!line.equals("may")))
                stopWords_Set.add(line);
        }
    }
    //getter
    public Set<String> getStopWords_Set(){
        return stopWords_Set;
    }
}
