package Model;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Indexer {

    private Processor processor; // pointer to Processor object
    private String mainPath;  // holding path of corpus
    private String savingPath; // holding path of saving dir
    public File tempPostingsForTerms; //dictionary for terms
    public File tempPostingsForCountry; //dictionary for terms
    static int termsFileIndex = 0; //index for temp posting of terms names
    static int countriesFileIndex = 0; //index for temp posting of countries names
    public Map<String,CountryInfo> countryMap; //map of countries and df <countryName, CountryInfo>
    private Map<String,String[]> earthCountries; // all countries in earth <capitalCity,[country,population,currency]>

    // C'tor
    public Indexer(Processor processor,String mainPath, String savingPath){
        this.processor = processor;
        this.mainPath = mainPath;
        this.savingPath = savingPath;

        //create dirs for tmp postings
        tempPostingsForTerms = new File(savingPath + "\\tempPostingForTerms");
        tempPostingsForTerms.mkdir();
        tempPostingsForCountry = new File(savingPath + "\\tempPostingsForCountries");
        tempPostingsForCountry.mkdir();

        //delete all previous files in directory
        final File[] files = tempPostingsForTerms.listFiles();
        for (File f : files)
            f.delete();

        //delete all previous files in directory
        final File[] files2 = tempPostingsForCountry.listFiles();
        for (File f1 : files2)
            f1.delete();

        countryMap = new HashMap<>();


        //making a MAP with all the values of the countries in the earth
        CreateMapOfAllCountries();
    }

    //making a MAP with all the values of the countries in the earth
    private void CreateMapOfAllCountries() {
        earthCountries = new HashMap<>();
        File toRead = new File("Resources/API_CountriesMap");
        try {
            //read the countries map from file
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
            earthCountries = (HashMap<String,String[]>)ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //--------------------------
    //---Create posting files---
    //--------------------------


    //create posting file to the terms
    public void addFileToTermIndex() throws IOException {
        //creating tmp file to posting
        File tmp_posting_file = new File(tempPostingsForTerms.getPath() + "\\" + termsFileIndex + ".txt");
        termsFileIndex++;

        //clean the docs for iteration for now..
        processor.readFile.docsForIteration = null;

        //sort the terms
        List<String> sortedTerms = new ArrayList<>(processor.parser.linesToPosting.keySet());
        // sorting sortedTerms without taking care if term is Lower/Upper Case. for example : gain , GAIN, gained
        Collections.sort(sortedTerms, (o1, o2) -> {
            o1 = o1.toLowerCase();
            o2 = o2.toLowerCase();
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return o1.compareTo(o2);
        });

        //go over every term from the sorted list
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tmp_posting_file, true)));
        //for each term in the sorted terms
        for (String term : sortedTerms) {
            //taking the line from the lines to posting map
            //the line is like:  doc1 freq1,doc2 freq2...
            StringBuilder lineOfTerm = processor.parser.linesToPosting.get(term);
            //add to posting
            out.println(term + ": " + lineOfTerm);

            String[] everyDocLine = lineOfTerm.toString().split(",");
            int numOfDocsToTerm = everyDocLine.length;

            //update the record in the dictionary
            TermInfo currTermInfo = processor.parser.allTerms_Map.get(term);

            currTermInfo.updateNumOfDocs(numOfDocsToTerm);
            currTermInfo.updateSumTF(processor.parser.termFrequency.get(term));
        }
        out.close();
    }

    //create posting file to the countries
    public void addFileToCountriesIndex() throws IOException {

        //creating tmp file to posting
        File tmp_posting_file = new File(tempPostingsForCountry.getPath() + "\\" + countriesFileIndex + ".txt");
        countriesFileIndex++;

        //sort the terms
        List<String> sortedCountries = new ArrayList<String>(processor.parser.lineToPosting_Countries.keySet());
        Collections.sort(sortedCountries);

        //go over every term from the sorted list
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tmp_posting_file, true)));
        //for each country in the sorted countries
        for (String country : sortedCountries) {
            //add to posting
            out.println(processor.parser.lineToPosting_Countries.get(country));
            //taking the line from the lines to posting map
            //the line is like:    term : doc1 freq1,doc2 freq2...
            String currPostingLine = processor.parser.lineToPosting_Countries.get(country).substring(processor.parser.lineToPosting_Countries.get(country).indexOf(":") + 2);
            String[] everyDocLine = currPostingLine.split(",");
            int numOfDocsToCounty = everyDocLine.length;

            //if there is no frequency in this doc to this term, put zero.. else take the frequency
            int tf_for_country = 0;
            if(processor.parser.termFrequency.containsKey(country))
                tf_for_country = processor.parser.termFrequency.get(country);

            //update the record in the dictionary
            if (countryMap.containsKey(country)) {
                CountryInfo infoToCountry = countryMap.get(country);
                infoToCountry.updateNumOfDocs(numOfDocsToCounty);
                infoToCountry.updateSumTF(infoToCountry.sumTf + tf_for_country);

                //if there is country like this in the earth map:
                if(earthCountries.containsKey(country)) {
                    String[] info = earthCountries.get(country);
                    infoToCountry.setCountry_name(info[0]);
                    infoToCountry.setPopulation(info[1]);
                    infoToCountry.setCorrency(info[2]);

                } else { //if it's not in the earth map, try use another API from web
                    String url = "http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + country;
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    String data = "";
                    JSONParser parser = new JSONParser();
                    try {
                        data = client.newCall(request).execute().body().string();
                        if(data != null && data.length() > 0) {
                            JSONObject jsonObject = (JSONObject) parser.parse(data);
                            if (!jsonObject.get("geobytescountry").equals("")) {
                                infoToCountry.setCountry_name((String) jsonObject.get("geobytescountry"));
                                infoToCountry.setCorrency((String) jsonObject.get("geobytescurrency"));
                                infoToCountry.setPopulation((String) jsonObject.get("geobytespopulation"));
                            }
                        }
                    } catch (IOException e) {
                    } catch (ParseException e) {
                    }

                }
            } else { //add to dictionary new record
                //if there country like this in the earth map:
                if (earthCountries.containsKey(country)) {
                    String[] info = earthCountries.get(country);
                    countryMap.put(country, new CountryInfo(info[0], info[1], info[2], numOfDocsToCounty, tf_for_country));
                }else
                    countryMap.put(country, new CountryInfo("","","", numOfDocsToCounty, tf_for_country));
            }
        }
        out.close();
    }

    //---------------------
    //---Merge functions---
    //---------------------

    //this function will manage and process the merge of the terms files
    public void mergePosting(String forWhat, File pathOfTemp) throws IOException{
        //create all posting files
        openPostingFiles(forWhat);

        File[] tempPosting1= pathOfTemp.listFiles();
        int amountOfTmp = pathOfTemp.listFiles().length;

        while (amountOfTmp > 2) {
            MergeTwoFiles(tempPosting1[0], tempPosting1[1],pathOfTemp,forWhat);
            tempPosting1= pathOfTemp.listFiles();
            amountOfTmp =  tempPosting1.length;
        }


        if(forWhat.equals("Terms")) {
            //merge the last two posting files of terms
            FinalMergeToTerms(tempPosting1[0], tempPosting1[1]);
            writeMapsToFile("TermsInfoMap");
        }
        else {
            //merge the last two posting files of cuntries
            FinalMergeToCountries(tempPosting1[0], tempPosting1[1]);
            writeMapsToFile("CountriesInfoMap");
        }

        //delete the temp directories
        tempPostingsForTerms.delete();
        tempPostingsForCountry.delete();
    }

    //merge two files into new one and delete the old ones
    private void MergeTwoFiles(File f1, File f2, File pathOfTemp,String forWhat) throws IOException {
        File posting;
        BufferedWriter br;
        FileOutputStream output;
        //check which index to increase
        if(forWhat.equals("Terms")) {
            posting = new File(pathOfTemp.getPath() + "\\" + termsFileIndex + ".txt");
            output = new FileOutputStream(posting);
            br = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            termsFileIndex++;
        }
        else{
            posting = new File(pathOfTemp.getPath() + "\\" + countriesFileIndex + ".txt");
            output = new FileOutputStream(posting);
            br = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            countriesFileIndex++;
        }
        PrintWriter out = new PrintWriter(br);

        // Open the file
        FileInputStream fs1 = new FileInputStream(f1);
        FileInputStream fs2 = new FileInputStream(f2);

        BufferedReader br1 = new BufferedReader(new InputStreamReader(fs1, StandardCharsets.UTF_8));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(fs2, StandardCharsets.UTF_8));

        //read first lines of both files
        String line1 = br1.readLine();
        String line2 = br2.readLine();

        //merge
        while (line1 != null && line2 != null && !line1.equals("") && !line2.equals(""))
        {
            String term1 = line1.substring(0,line1.indexOf(':')).toLowerCase();
            String term2 = line2.substring(0,line2.indexOf(':')).toLowerCase();
            int compareNum = term1.compareTo(term2); //comparing terms
            if(compareNum > 0)//if line 1 > line 2 ---> we write line 2
            {
                out.println(line2);
                line2 = br2.readLine();
            }
            else if(compareNum < 0)//if line 1 < line 2 ---> we write line 1
            {
                out.println(line1);
                line1 = br1.readLine();
            }
            else if(compareNum == 0)// if line 1 == line 2 ---> we merge both lines to one line
            {
                out.println(line1 + "," + (line2.substring(line2.indexOf(':') + 2)));
                line1 = br1.readLine();
                line2 = br2.readLine();
            }

        }

        // only first file left case
        while(line1 != null)
        {
            out.println(line1);
            line1 = br1.readLine();
        }
        // only second file left case
        while(line2 != null)
        {
            out.println(line2);
            line2 = br2.readLine();
        }

        //close all files
        fs1.close();
        fs2.close();
        br1.close();
        br2.close();
        out.close();

        br.close();
        output.close();

        //delete f1 f2
        f1.delete();
        f2.delete();
    }

    // this function will open files to all letters and one for numbers
    private void openPostingFiles(String forWhat) throws IOException {
        //create dir for final posting terms
        File postingDir = new File(savingPath+"\\PostingFor" + forWhat);
        postingDir.mkdir();
        //delete last files if exists
        File[] files = postingDir.listFiles();
        for (File f : files)
            f.delete();

        // init names of posting files of terms
        String[] postingsName = {"numbers", "a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
        // start making files
        for (String fileName:postingsName)
        {
            //if it's counties posting, do not create numbers.txt (and make name in upper case text)
            if(forWhat.equals("Countries")){
                fileName = fileName.toUpperCase();
                if(fileName.equals("NUMBERS"))
                    continue;
            }
            File f = new File(postingDir.getPath()+"\\"+fileName+".txt");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
            out.close();
        }
    }

    //-----------------
    //-----Final-------
    //-----Merge-------
    //-----------------

    //this is the final merge, it merge the last two files that left
    private void FinalMergeToTerms(File f1, File f2) throws IOException {
        // Open the file
        FileInputStream fs1 = new FileInputStream(f1);
        FileInputStream fs2 = new FileInputStream(f2);

        BufferedReader br1 = new BufferedReader(new InputStreamReader(fs1, StandardCharsets.UTF_8));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(fs2, StandardCharsets.UTF_8));

        String line1 = br1.readLine();
        String line2 = br2.readLine();

        File f = new File(savingPath+"\\PostingForTerms\\numbers.txt");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
        char lastChar = '0'; //represent the last posting file we wrote to
        int pointerToFile = 0;

        //merge
        while (line1 != null && line2 != null && !line1.equals("") && !line2.equals(""))
        {
            String term1_Origin = line1.substring(0,line1.indexOf(':'));
            String term2_Origin = line2.substring(0,line2.indexOf(':'));
            String term1 = term1_Origin.toLowerCase();
            String term2 = term2_Origin.toLowerCase();

            int compareNum = term1.compareTo(term2); //comparing terms
            if(compareNum > 0)// if line 1 > line 2 ---> we write line 2
            {
                char firstChar_line2 = term2.charAt(0);
                if(lastChar != firstChar_line2 && (firstChar_line2 >= 'a' && firstChar_line2 <= 'z'))
                {
                    lastChar = firstChar_line2;
                    out.close();
                    f = new File(savingPath+"\\PostingForTerms\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0; //reset the pointer
                }

                //if the term is not with lower case:
                if((!processor.parser.allTerms_Map.containsKey(term2))) {
                    term2 = term2.toUpperCase();
                    //if it's not with upper case:
                    if (!processor.parser.allTerms_Map.containsKey(term2)) {
                        term2 = term2_Origin; //take the origin
                    }
                }

                out.println(line2);
                TermInfo currTermInfo = processor.parser.allTerms_Map.get(term2);
                currTermInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = line2.getBytes().length;
                currTermInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

                line2 = br2.readLine();
            }
            else if(compareNum < 0)// if line 1 < line 2 ---> we write line 1
            {
                char firstChar_line1 = term1.charAt(0);
                if(lastChar != firstChar_line1 && (firstChar_line1 >= 'a' && firstChar_line1 <= 'z'))
                {
                    lastChar = firstChar_line1;
                    out.close();
                    f = new File(savingPath+"\\PostingForTerms\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0;
                }

                //if the term is not with lower case:
                if((!processor.parser.allTerms_Map.containsKey(term1))) {
                    term1 = term1.toUpperCase();
                    //if it's not with upper case:
                    if (!processor.parser.allTerms_Map.containsKey(term1)) {
                        term1 = term1_Origin; //take the origin
                    }
                }

                out.println(line1);
                TermInfo currTermInfo = processor.parser.allTerms_Map.get(term1);
                currTermInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = line1.getBytes().length;
                currTermInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer
                currTermInfo.calculateIdf();

                line1 = br1.readLine();
            }
            else if(compareNum == 0)// if line1 == line 2 --> merge both lines to one line
            {
                char firstChar_line1 = term1.charAt(0);
                if(lastChar != firstChar_line1 && (firstChar_line1 >= 'a' && firstChar_line1 <= 'z'))
                {
                    lastChar = firstChar_line1;
                    out.close();
                    f = new File(savingPath+"\\PostingForTerms\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0;
                }

                //if the term is not with lower case:
                if((!processor.parser.allTerms_Map.containsKey(term1))) {
                    term1 = term1.toUpperCase();
                    //if it's not with upper case:
                    if (!processor.parser.allTerms_Map.containsKey(term1)) {
                        term1 = term1_Origin; //take the origin
                    }
                }

                //connecting the lines
                String bothLines = line1 + "," + (line2.substring(line2.indexOf(':') + 2));
                out.println(bothLines);
                TermInfo currTermInfo = processor.parser.allTerms_Map.get(term1);
                currTermInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = bothLines.getBytes().length;
                currTermInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer
                currTermInfo.calculateIdf();

                line1 = br1.readLine();
                line2 = br2.readLine();
            }
        }

        // only first file left case
        while(line1 != null)
        {
            String term1_Origin = line1.substring(0,line1.indexOf(':'));
            String term1 = term1_Origin.toLowerCase();
            char firstChar_line1 = term1.charAt(0);

            //if the term is not with lower case:
            if((!processor.parser.allTerms_Map.containsKey(term1))) {
                term1 = term1.toUpperCase();
                //if it's not with upper case:
                if (!processor.parser.allTerms_Map.containsKey(term1)) {
                    term1 = term1_Origin; //take the origin
                }
            }

            if(lastChar != firstChar_line1 && (firstChar_line1 >= 'a' && firstChar_line1 <= 'z'))
            {
                lastChar = firstChar_line1;
                out.close();
                f = new File(savingPath+"\\PostingForTerms\\"+lastChar+".txt");
                out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                pointerToFile = 0;
            }

            out.println(line1);
            TermInfo currTermInfo = processor.parser.allTerms_Map.get(term1);
            currTermInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
            int lengthOfLine = line1.getBytes().length;
            currTermInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
            pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer
            currTermInfo.calculateIdf();

            line1 = br1.readLine();
        }
        // only second file left case
        while(line2 != null)
        {
            String term2_Origin = line2.substring(0,line2.indexOf(':'));
            String term2 = term2_Origin.toLowerCase();
            char firstChar_line2 = term2.charAt(0);

            //if the term is not with lower case:
            if((!processor.parser.allTerms_Map.containsKey(term2))) {
                term2 = term2.toUpperCase();
                //if it's not with upper case:
                if (!processor.parser.allTerms_Map.containsKey(term2)) {
                    term2 = term2_Origin; //take the origin
                }
            }

            if(lastChar != firstChar_line2 && (firstChar_line2 >= 'a' && firstChar_line2 <= 'z'))
            {
                lastChar = firstChar_line2;
                out.close();
                f = new File(savingPath+"\\PostingForTerms\\"+lastChar+".txt");
                out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                pointerToFile = 0;
            }

            out.println(line2);
            TermInfo currTermInfo = processor.parser.allTerms_Map.get(term2);
            currTermInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
            int lengthOfLine = line2.getBytes().length;
            currTermInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
            pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer
            currTermInfo.calculateIdf();

            line2 = br2.readLine();
        }


        fs1.close();
        fs2.close();
        br1.close();
        br2.close();
        out.close();

        //delete f1 f2
        f1.delete();
        f2.delete();
    }

    //this is the final merge, it merge the last two files that left
    private void FinalMergeToCountries(File f1, File f2) throws IOException {
        // Open the file
        FileInputStream fs1 = new FileInputStream(f1);
        FileInputStream fs2 = new FileInputStream(f2);

        BufferedReader br1 = new BufferedReader(new InputStreamReader(fs1, StandardCharsets.UTF_8));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(fs2, StandardCharsets.UTF_8));

        String line1 = br1.readLine();
        String line2 = br2.readLine();

        File f = new File(savingPath+"\\PostingForCountries\\A.txt");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));

        char lastChar = 'A'; //represent the last posting file we wrote to
        int pointerToFile = 0;
        //merge
        while (line1 != null && line2 != null && !line1.equals("") && !line2.equals(""))
        {
            String term1 = line1.substring(0,line1.indexOf(':'));
            String term2 = line2.substring(0,line2.indexOf(':'));

            int compareNum = term1.compareTo(term2); //comparing terms
            if(compareNum > 0)// if line 1 > line 2 ---> we write line 2
            {
                char firstChar_line2 = term2.charAt(0);
                if(lastChar != firstChar_line2 && (firstChar_line2 >= 'A' && firstChar_line2 <= 'Z'))
                {
                    lastChar = firstChar_line2;
                    out.close();
                    f = new File(savingPath+"\\PostingForCountries\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0;
                }

                out.println(line2);
                CountryInfo currCountryInfo = countryMap.get(term2);
                currCountryInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = line2.getBytes().length;
                currCountryInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

                line2 = br2.readLine();
            }
            else if(compareNum < 0)// if line 1 < line 2 ---> we write line 1
            {
                char firstChar_line1 = term1.charAt(0);
                if(lastChar != firstChar_line1 && (firstChar_line1 >= 'A' && firstChar_line1 <= 'Z'))
                {
                    lastChar = firstChar_line1;
                    out.close();
                    f = new File(savingPath+"\\PostingForCountries\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0;
                }

                out.println(line1);
                CountryInfo currCountryInfo = countryMap.get(term1);
                currCountryInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = line1.getBytes().length;
                currCountryInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

                line1 = br1.readLine();
            }
            else if(compareNum == 0)// if line1 == line 2 --> merge both lines to one line
            {
                char firstChar_line1 = term1.charAt(0);
                if(lastChar != firstChar_line1 && (firstChar_line1 >= 'A' && firstChar_line1 <= 'Z'))
                {
                    lastChar = firstChar_line1;
                    out.close();
                    f = new File(savingPath+"\\PostingForCountries\\"+lastChar+".txt");
                    out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                    pointerToFile = 0;
                }

                String bothLines = line1 + "," + (line2.substring(line2.indexOf(':') + 2));
                out.println(bothLines);
                CountryInfo currCountryInfo = countryMap.get(term1);
                currCountryInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
                int lengthOfLine = bothLines.getBytes().length;
                currCountryInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
                pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

                line1 = br1.readLine();
                line2 = br2.readLine();
            }
        }

        // only first file left case
        while(line1 != null)
        {
            String term1 = line1.substring(0,line1.indexOf(':'));
            char firstChar_line1 = term1.charAt(0);

            if(lastChar != firstChar_line1 && (firstChar_line1 >= 'A' && firstChar_line1 <= 'Z'))
            {
                lastChar = firstChar_line1;
                out.close();
                f = new File(savingPath+"\\PostingForCountries\\"+lastChar+".txt");
                out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                pointerToFile = 0;
            }

            out.println(line1);
            CountryInfo currCountryInfo = countryMap.get(term1);
            currCountryInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
            int lengthOfLine = line1.getBytes().length;
            currCountryInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
            pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

            line1 = br1.readLine();
        }
        // only second file left case
        while(line2 != null)
        {
            String term2 = line2.substring(0,line2.indexOf(':'));
            char firstChar_line2 = term2.charAt(0);

            if(lastChar != firstChar_line2 && (firstChar_line2 >= 'A' && firstChar_line2 <= 'Z'))
            {
                lastChar = firstChar_line2;
                out.close();
                f = new File(savingPath+"\\PostingForCountries\\"+lastChar+".txt");
                out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                pointerToFile = 0;
            }

            out.println(line2);
            CountryInfo currCountryInfo = countryMap.get(term2);
            currCountryInfo.setLineInPosting(pointerToFile);//set the pointer to the term in the file
            int lengthOfLine = line2.getBytes().length;
            currCountryInfo.setLengthInFile(lengthOfLine);//set the length of the curr term line in file
            pointerToFile = pointerToFile + lengthOfLine + 2; //increase the pointer (+2 because we have \n)

            line2 = br2.readLine();
        }


        fs1.close();
        fs2.close();
        br1.close();
        br2.close();
        out.close();

        //delete f1 f2
        f1.delete();
        f2.delete();
    }

    //-------------------
    //-----Write Maps----
    //-------------------

    //write the information about the documents to file
    private void writeMapsToFile(String fileName) throws IOException {
        File MapFile = new File(savingPath+"\\" + fileName);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(MapFile));

        //choose which object to write
        if(fileName.equals("TermsInfoMap")) {
            out.writeObject(processor.parser.allTerms_Map);
            out.close();
        }
        else {
            out.writeObject(countryMap);
            out.close();
            writeDocsMapToFile();
        }
    }

    //this function writes the all docs map to file
    private void writeDocsMapToFile() throws IOException {
        File MapFile = new File(savingPath+"\\DocsInfoMap");
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(MapFile));

        //write the map of docs to file:
        out.writeObject(processor.readFile.allDocs_Map);
        out.close();
    }
}

