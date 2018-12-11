package Model;

import java.io.Serializable;

public class CountryInfo implements Serializable{

    private String country_name; // name of country
    private String corrency; // currency used in country
    private String population; // num of pop in country
    int df; // document freq
    double idf; // idf
    int lineInPosting; // location of the term in posting file
    int lengthInFile; // length in bytes of the line in posting file
    int sumTf; // sum term freq in all Corpus
    // C'tor
    public CountryInfo(String country_name, String population,String corrency,int numOfDocs, int sumTf) {
        this.country_name = country_name;
        this.corrency = corrency;
        this.population = population;
        this.sumTf = sumTf;
        this.df = numOfDocs;
    }

    // update number of docs (df)
    public void updateNumOfDocs(int num) {
        this.df += num;
    }

    // update sum of term freq (sumTf)
    public void updateSumTF(int num) {
        if(this.sumTf != -1)
            this.sumTf += num;
        else
            this.sumTf = num;
    }

    // setter
    public void setCorrency(String corrency) {
        this.corrency = corrency;
    }

    // getter
    public String getPopulation() {
        return population;
    }

    // setter
    public void setPopulation(String population) {
        this.population = population;
    }

    // setter
    public void setLineInPosting(int lineInPosting) {
        this.lineInPosting = lineInPosting;
    }

    // setter
    public void setLengthInFile(int lengthInFile) {
        this.lengthInFile = lengthInFile;
    }

    public String getCountry_name() {
        return country_name;
    }

    public void setCountry_name(String country_name) {
        this.country_name = country_name;
    }

    public String getCorrency() {
        return corrency;
    }
}
