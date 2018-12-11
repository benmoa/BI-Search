package Model;

import java.io.Serializable;

public class TermInfo implements Comparable<TermInfo>,Serializable{
    String term; // name of term
    int df; // freq in doc
    double idf; // idf
    int lineInPosting; // location of the term in posting file
    int lengthInFile; // length in bytes of the line in posting file
    int sumTf; // sum term freq in all Corpus

    // C'tor
    public TermInfo(String term, int df, int sumTf) {
        this.df = df;
        this.lineInPosting = -1;
        idf=0;
        this.term = term;
        this.sumTf = sumTf;
    }
    // update df
    public void updateNumOfDocs(int num) {
        if(this.df != -1)
            this.df += num;
        else
            this.df = num;
    }
    // update sum tf
    public void updateSumTF(int num) {
        if(this.sumTf != -1)
            this.sumTf += num;
        else
            this.sumTf = num;
    }
    // setter
    public void setLineInPosting(int lineInPosting) {
        this.lineInPosting = lineInPosting;
    }
    // setter
    public void setLengthInFile(int lengthInFile) {
        this.lengthInFile = lengthInFile;
    }
    //compare by the number of docs handle this term
    @Override
    public int compareTo(TermInfo o) {
        if(this.df > o.df)
            return 1;
        if(this.df < o.df)
            return -1;
        return 0;
    }
    // getter
    public String getTerm() {
        return term;
    }
    // getter
    public int getSumTf() {
        return sumTf;
    }
    // getter
    public int getDf() {
        return df;
    }
    // calc idf by using idf and df
    public void calculateIdf()
    {
        idf=Math.log(468360/ df)/Math.log(2);
    }
    // to string function
    public String toString()
    {
        return (term + " : " + df + " " + sumTf + " " + lineInPosting);
    }

}
