package Model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Document implements Serializable{

    private String file_path; //the path of the file that the doc is in
    private int start_line; //the line number that the doc starts
    private String doc_number; //the doc number inside the file
    private String file_name; //the name of the file that the doc is in
    private Date date; //the date
    private String title;//the title of the doc
    private String country; // the country of this doc
    private int numOfTerms; //count the number of terms in the doc
    private int max_tf; // maximum freq in this doc
    private String mostFreqTerm; //the most frequency term in the doc
    private String placesOfMaxTF; //string of places of the max tf in this doc.. like: "-1!4!2..."
    private float sumWeights; //sum of all (tf*idf)^2

    // C'tor
    public Document() {
        file_path = "";
        start_line = -1;
        doc_number = "";
        file_name = "";
        date = null;
        title = "";
        country = "";
        numOfTerms = -1;
        max_tf = -1;
        mostFreqTerm = "";
    }
    // C'tor
    public Document(String file_path, int start_line, String doc_number, Date date, String title,String country) {
        this.file_path = file_path;
        this.start_line = start_line;
        this.doc_number = doc_number;
        this.date = date;
        this.title = title;
        this.country = country;
        this.max_tf = 0;
    }

    // setter
    public void setFile_path(String file_path) {
        this.file_path = file_path;

        //setting the name of file
        String name = file_path.substring(file_path.indexOf("corpus\\") + 7);
        name = name.substring(name.indexOf("\\") + 1);
        setFile_name(name);
    }
    // setter
    public void setStart_line(int start_line) {
        this.start_line = start_line;
    }
    // setter
    public void setDoc_number(String doc_number) {
        this.doc_number = doc_number;
    }
    // setter
    public void setPlacesOfMaxTF(String placesOfMaxTF) {
        this.placesOfMaxTF = placesOfMaxTF;
    }
    // setter
    public void setCountry(String country) {
        this.country = country;
    }
    // setter
    public void setDate(String date) {
        DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
        try {
            this.date = format.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // setter
    public void setTitle(String title) {
        this.title = title;
    }
    // setter
    public void setMostFreqTerm(String mostFreqTerm) {
        this.mostFreqTerm = mostFreqTerm;
    }
    // setter
    public void setNumOfTerms(int numOfTerms) {
        this.numOfTerms = numOfTerms;
    }
    // getter
    public String getCountry() {
        return country;
    }
    // getter
    public String getFile_name() {
        return file_name;
    }
    // getter
    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }
    // getter
    public String getDoc_number() {
        return doc_number;
    }

    @Override
    // to string func
    public String toString() {
        return "Document{" +
                "file_path='" + file_path + '\'' +
                ", start_line=" + start_line +
                ", docNumber='" + doc_number + '\'' +
                ", date=" + date +
                ", title='" + title + '\'' +
                '}';
    }


    public int getMax_tf() {
        return max_tf;
    }

    public void setMax_tf(int max_tf) {
        this.max_tf = max_tf;
    }

    public int getNumOfTerms() {
        return numOfTerms;
    }

    public String getMostFreqTerm() {
        return mostFreqTerm;
    }

    public String getPlacesOfMaxTF() {
        return placesOfMaxTF;
    }

    public float getSumWeights() {
        return sumWeights;
    }

    public void updateSumWeights(float sumWeights) {
        this.sumWeights += sumWeights;
    }
}