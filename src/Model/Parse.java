package Model;

import javafx.util.Pair;

import java.util.*;

public class Parse {
    private Processor processor; // pointer to Processor object
    private StopWords stopWords;  // holds all stop words
    public Map<String,TermInfo> allTerms_Map; //map of ALL terms and df:  <term,termInfo>
    private Map<String,String> monthes; // holds all names of months and thier number values
    private Set<Character> deleteList; //list of characters to delete
    public Map<String,Integer> termFrequency; // each term and hes frequency  <term, freq>
    public Map<String, StringBuilder> linesToPosting; // <termName, term: docName freq,docName freq...>
    public Map<String,StringBuilder> lineToPosting_Countries; //lines to the posting file that contains <countryName,line_to_post> look like: "israel : doc1 4,doc92 5..."
    private String mainPath;  // holding path of corpus
    private String savingPath; // holding path of saving dir
    private boolean doStemming; // if to do stem
    private int placeInDoc; //is the index of term in the doc (the place of the term in the doc)
    private static int numOfTerms; // terms counter for each Doc
    private Map<String, Pair<Integer,String>> tfForDoc; // Map of Terms for only one Doc and their tf and places in doc <term,<tf,places_in_doc>>
    private Map<String,String> largeNumbers; //this map will save all the Billions, Millions etc...


    // C'tor
    public Parse(Processor processor,String mainPath ,String savingPath, boolean doStemming) {
        this.processor = processor;
        this.mainPath = mainPath;
        this.savingPath = savingPath;
        this.doStemming = doStemming;
        this.stopWords = new StopWords(mainPath);
        this.termFrequency = new HashMap<>();
        this.linesToPosting = new HashMap<>();
        this.lineToPosting_Countries = new HashMap<>();
        this.allTerms_Map = new HashMap<>();
        initMonthes();
        initDeleteList();
        initLargeNumbers();
    }
    // init list of signs to remove from terms
    private void initDeleteList(){
        deleteList = new HashSet<>();
        deleteList.add(',');
        deleteList.add('.');
        deleteList.add('!');
        deleteList.add(')');
        deleteList.add('(');
        deleteList.add(']');
        deleteList.add('[');
        deleteList.add('{');
        deleteList.add('}');
        deleteList.add(',');
        deleteList.add('*');
        deleteList.add('&');
        deleteList.add('$');
        deleteList.add(':');
        deleteList.add('\'');
        deleteList.add(';');
        deleteList.add('/');
        deleteList.add('#');
        deleteList.add('?');
        deleteList.add('+');
        deleteList.add('"');
        deleteList.add('|');
        deleteList.add('`');
        deleteList.add('\\');
        deleteList.add(' ');
        deleteList.add('-');
        deleteList.add('@');
        deleteList.add('^');
        deleteList.add('=');
        deleteList.add('~');
        deleteList.add('_');
    }
    // init map of months
    private void initMonthes() {
        monthes=new HashMap<String,String>();
        monthes.put("January","01");
        monthes.put("Jan","01");
        monthes.put("JANUARY","01");
        monthes.put("JAN","01");
        monthes.put("january","01");
        monthes.put("jan","01");

        monthes.put("Febuary","02");
        monthes.put("Feb","02");
        monthes.put("FEBUARY","02");
        monthes.put("FEB","02");
        monthes.put("febuary","02");
        monthes.put("feb","02");

        monthes.put("March","03");
        monthes.put("Mar","03");
        monthes.put("MARCH","03");
        monthes.put("MAR","03");
        monthes.put("march","03");
        monthes.put("mar","03");

        monthes.put("April","04");
        monthes.put("Apr","04");
        monthes.put("APRIL","04");
        monthes.put("APR","04");
        monthes.put("april","04");
        monthes.put("apr","04");

        monthes.put("May","05");
        monthes.put("MAY","05");
        monthes.put("may","05");

        monthes.put("June","06");
        monthes.put("Jun","06");
        monthes.put("JUNE","06");
        monthes.put("JUN","06");
        monthes.put("june","06");
        monthes.put("jun","06");

        monthes.put("July","07");
        monthes.put("Jul","07");
        monthes.put("JULY","07");
        monthes.put("JUL","07");
        monthes.put("july","07");
        monthes.put("jul","07");

        monthes.put("August","08");
        monthes.put("Aug","08");
        monthes.put("AUGUST","08");
        monthes.put("AUG","08");
        monthes.put("august","08");
        monthes.put("aug","08");

        monthes.put("September","09");
        monthes.put("Sep","09");
        monthes.put("Sept","09");
        monthes.put("SEPTEMBER","09");
        monthes.put("SEP","09");
        monthes.put("september","09");
        monthes.put("sep","09");
        monthes.put("sept","09");

        monthes.put("October","10");
        monthes.put("Oct","10");
        monthes.put("OCTOBER","10");
        monthes.put("OCT","10");
        monthes.put("october","10");
        monthes.put("oct","10");

        monthes.put("November","11");
        monthes.put("Nov","11");
        monthes.put("NOVEMBER","11");
        monthes.put("NOV","11");
        monthes.put("november","11");
        monthes.put("nov","11");

        monthes.put("December","12");
        monthes.put("Dec","12");
        monthes.put("DECEMBER","12");
        monthes.put("DEC","12");
        monthes.put("december","12");
        monthes.put("dec","12");
    }
    // init map of large numbers
    private void initLargeNumbers() {
        this.largeNumbers = new HashMap<>();
        largeNumbers.put("Thousand","K");
        largeNumbers.put("Thousands","K");
        largeNumbers.put("thousand","K");
        largeNumbers.put("thousands","K");
        largeNumbers.put("million","M");
        largeNumbers.put("millions","M");
        largeNumbers.put("Million","M");
        largeNumbers.put("Millions","M");
        largeNumbers.put("Billion","B");
        largeNumbers.put("Billions","B");
        largeNumbers.put("billion","B");
        largeNumbers.put("billions","B");
        largeNumbers.put("Trillion","00B");
        largeNumbers.put("Trillions","00B");
        largeNumbers.put("trillion","00B");
        largeNumbers.put("trillions","00B");

    }
    // converting from string to float
    private float stringToFloat (String str) {
        return Float.parseFloat(removeCommaFromString(str));
    }
    // checking if string includes only numbers or "/" or "."
    private boolean isNumeric(String str) {
        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c) && (!(c == '.')) && !(c == '/')) {
                return false;}
        }
        return true;
    }
    // checking if string includes only numbers
    public static boolean isNumber(String str) {
        for (char c : str.toCharArray())
            if (!Character.isDigit(c))
                return false;
        return true;
    }
    // checking if string includes only letters
    private boolean isAlpha(String str){
        return str.chars().allMatch(Character::isLetter);
    }
    // checking if string is percent or percentage
    private boolean isPercent (String str){
        return  str.equals("percent") || str.equals("percentage") ;
    }
    // checking by using monthes map if string is name of month
    private boolean isDate (String str) {
        return monthes.containsKey(str);
    }
    // converting string to int
    private int stringToInt (String str) {
        if (str.equals("")){
            return -1;
        }
        if (hasPointAtEnd(str)){
            str = str.substring(0,str.length()-1);
            if(!str.equals(""))
                return Integer.parseInt(str);
            else
                return -1;
        }
        return Integer.parseInt(str);
    }
    //converting string to double
    private double stringToDouble (String str) {
        if (str.equals("")){
            return -1;
        }
        if (hasPointAtEnd(str)){
            str = str.substring(0,str.length()-1);
            if(!str.equals(""))
                return Double.parseDouble(str);
            else
                return -1;
        }
        return Double.parseDouble(str);
    }
    //checking if string is million or billion (all kinds)
    private boolean isMilOrBilOrTril (String str) {
        return str.equals("million") || str.equals("m") || str.equals("billion") || str.equals("bn") || str.equals("trillion");
    }
    // converting string to long
    private long stringToLong (String str) {
        if (str.equals("")){
            return -1;
        }
        if (hasPointAtEnd(str)){
            str = str.substring(0,str.length()-1);
            if(!str.equals(""))
                return Long.valueOf(removeCommaFromString(str));
            else
                return -1;
        }
        try {
            return Long.valueOf(removeCommaFromString(str));
        }catch (NumberFormatException o){
            return -1;
        }
    }
    // checking if string includes only capital letters
    private boolean isAllCapitalInString (String str) {
        for (int i=0 ; i < str.length() ; i++) {
            if (!(Character.isUpperCase(str.charAt(i))))
                return false;
        }
        return true;
    }
    // this function will remove comma from string and return it
    private String removeCommaFromString (String str) {
        return str.replace(",", "");
    }
    // checking if string is name of country
    private boolean isCountry(String str) {
        for (char ch : str.toCharArray()){
            //if it's not a letter:
            if(!Character.isLetter(ch)){
                //it can be some things..
                if((!(ch == '\'')) && (!(ch == '-')))
                    return false;
            }
        }
        return true;
    }
    // checking if first char is capital letter
    private boolean isFirstCharIsCapital (String str) { return Character.isUpperCase(str.charAt(0)); }
    // this function will return string with capital letters
    private String toCapitalLetters (String str) {return str.toUpperCase();}
    //checking if string includes in allTerm_map as key
    private boolean isTermExists (String str) {
        return allTerms_Map.containsKey(str);
    }
    // checking length of number
    private int checkSizeOfNum (String str) {
        int counter = 0;
        if (Character.getNumericValue(str.charAt(0)) > 0){
            for (int i = 0 ; i < str.length() ; i ++) {
                if (Character.isDigit(str.charAt(i)))
                    counter++;

                if(str.charAt(i) == ',')
                    continue;

            }
        }
        return counter;
    }
    // checking if next tokens is about $
    private boolean isNextTokensDollars (String[] textOfDoc, int tokenIndex,int sizeOfDoc) {
        int i = 0;
        while( (i < 4) && (tokenIndex < sizeOfDoc)){
            if(textOfDoc [tokenIndex].equals("Dollars") ||textOfDoc [tokenIndex].equals("dollars")){
                return true;
            }
            i++;
            tokenIndex++;
        }
        return false;
    }
    // this function will check if current token and nexts tokens is about date
    private void handleDate(String currToken, String[] textOfDoc, int tokenIndex, int sizeOfDoc,String docName) {
        if ((tokenIndex + 1 < sizeOfDoc) && isNumber(textOfDoc[tokenIndex + 1])) { // if the first value is MONTH and the sec is NUMBER

            // if the token is day
            if ((tokenIndex + 1 < sizeOfDoc) && stringToInt(textOfDoc[tokenIndex + 1]) >= 1 && stringToInt(textOfDoc[tokenIndex + 1]) <= 31){
                updateTermFreq(monthes.get(currToken) + "-" + textOfDoc[tokenIndex + 1]);
            }
            // if the token is 31 to 100 ---> 1931 to 2000
            else if ((tokenIndex + 1 < sizeOfDoc) && stringToInt(textOfDoc[tokenIndex + 1]) > 31 && stringToInt(textOfDoc[tokenIndex + 1]) < 100){
                updateTermFreq("19" + textOfDoc[tokenIndex + 1] + "-" + monthes.get(currToken));
            }
            else if ((tokenIndex + 1 < sizeOfDoc)) { // if token over 100
                updateTermFreq(textOfDoc[tokenIndex + 1] + "-" + monthes.get(currToken));
            }
            else {
                updateTermFreq(currToken);
            }
        }

        else if ((tokenIndex + 1 < sizeOfDoc)) { // if its date (MM-DD) case --> 14 MAY to 05-14
            updateTermFreq(monthes.get(textOfDoc[tokenIndex + 1]) + "-" + currToken);
        }
        else {
            updateTermFreq(currToken);
        }

    }
    // check if there is no < > -- in token
    private boolean isLegalToken (String currToken) {
        return (!(stopWords.getStopWords_Set().contains(currToken.toLowerCase()))) && (currToken.length() > 0)
                &&  (!(currToken.equals(""))
                && (!(currToken.equals("--"))) && (!(currToken.charAt(0) == '<'))
                && (!(currToken.charAt(currToken.length()-1) == '>')));
    }
    //this function will get Document and parse it
    public void ParseThisDoc(String documentText, String docName, String docFileName){

        // reset numOfTerms,tfForDoc and tokenIndex to empty and get ready to new Document
        numOfTerms = 0;
        tfForDoc = new HashMap<>();
        int tokenIndex = 0;
        placeInDoc = 0;

        String currToken = "";

        //this line makes token from current doc
        String[] textOfDoc = getTokensFromText(documentText);
        int sizeOfDoc = textOfDoc.length;

        //while there are more tokens
        while (tokenIndex < sizeOfDoc){
            currToken = textOfDoc[tokenIndex];
            if (isLegalToken(currToken)){ //check if the token is OK for parse
                currToken = removeUnnecessaryChars(currToken);

                if (currToken.equals("")) {
                    tokenIndex++;
                    continue;
                }

                //System.out.println(currToken);

                if (isAlpha(currToken)) { // if currrToken includes only letters
                    if (isDate(currToken)) { // if its date case
                        handleDate(currToken,textOfDoc,tokenIndex,sizeOfDoc,docName);
                        tokenIndex += 2;
                        continue;
                    }
                    if (isFirstCharIsCapital(currToken)) { //first char is capital case, like "First"

                        if (isAllCapitalInString(currToken)) { // if all letters is capital, just add it as is
                            if(isTermExists(currToken.toLowerCase())) { //if there is same term in map, but with lower case
                                currToken = currToken.toLowerCase(); //change it to lower case
                            }
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }
                        else { // term is first letter is Capital like "First"
                            if(isTermExists(currToken.toLowerCase())) { //if there is same term in map, but with lower case
                                currToken = currToken.toLowerCase(); //change it to lower case
                            }
                            else{
                                currToken = currToken.toUpperCase(); //change it to upper case
                            }
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }
                    }
                    //term is not with upper case in the first char
                    // if term exists on map (with capital letters), replace it with small letters
                    else if (isTermExists(toCapitalLetters(currToken))){
                        updateLowerUpperCases(currToken);
                        tokenIndex++;
                        continue;
                    }
                    // between case. for example : between 4 and 20
                    if (currToken.equals("between")){
                        if ((tokenIndex + 3 < sizeOfDoc) && (isNumeric(textOfDoc [tokenIndex + 1]))
                                && (textOfDoc [tokenIndex + 2].equals("and")) &&
                                (isNumeric(textOfDoc [tokenIndex + 3]))) {
                            updateTermFreq(currToken + " " + textOfDoc [tokenIndex + 1] +
                                    " and " + textOfDoc [tokenIndex + 3]);
                            tokenIndex += 4;
                            continue;
                        }
                        else { // if token is between but the next is not numbers. for example: between mar and nov
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }


                    }
                    updateTermFreq(currToken);
                    tokenIndex++;
                    continue;
                } // end of only letters case


                else if (Character.isDigit(currToken.charAt(0))) { // if the first char is digit
                    // pay attention!! we also remove Comma here!
                    if (isNumeric(removeCommaFromString(currToken))) { // if the all token is number
                        if (currToken.contains("/")){ // if curr has / -> add it as is
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }

                        if(hasMoreThanOnePoint(currToken)){ //if the token have more than one point, like: 11.4.1994
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }
                        if (stringToDouble(removeCommaFromString(currToken)) < 1000000) { // if num is under 1 million
                            // check if its not Dollars case
                            if (!(isNextTokensDollars(textOfDoc,tokenIndex,sizeOfDoc))){
                                if ((stringToDouble(removeCommaFromString(currToken)) > 1000)){// NUM K case
                                    double number = stringToDouble(removeCommaFromString(currToken));
                                    updateTermFreq(number / 1000 + "K");
                                    tokenIndex++;
                                    continue;
                                }

                                // Thousands / Million / Billion / Trillion  case
                                if ((tokenIndex + 1 < sizeOfDoc) && (largeNumbers.containsKey(textOfDoc[tokenIndex + 1])) ){
                                    //updateTermFreq(currToken + "K");
                                    updateTermFreq(currToken + largeNumbers.get(textOfDoc[tokenIndex + 1]));
                                    tokenIndex +=2;
                                    continue;
                                }

                                // two numbers in a row, example : 55 3/4
                                if ((tokenIndex + 1 < sizeOfDoc) && (isNumeric(textOfDoc [tokenIndex + 1]))) {
                                    updateTermFreq(currToken + " " + textOfDoc [tokenIndex + 1]);
                                    tokenIndex +=2;
                                    continue;
                                }
                                if ((tokenIndex + 1 < sizeOfDoc) && isPercent(textOfDoc[tokenIndex + 1])) {// if its percent case
                                    updateTermFreq(currToken + "%");
                                    tokenIndex += 2;
                                    continue;
                                }

                                if ((tokenIndex + 1 < sizeOfDoc) && isDate(textOfDoc[tokenIndex + 1])) { // if its date (MM-DD) case --> 14 MAY to 05-14
                                    handleDate(currToken, textOfDoc, tokenIndex, sizeOfDoc, docName);
                                    tokenIndex += 2;
                                    continue;
                                }
                                updateTermFreq(currToken);
                                tokenIndex++;
                                continue;

                            }

                            // case like 70 Dollars
                            if ((tokenIndex + 1 < sizeOfDoc) && textOfDoc[tokenIndex + 1].equals("Dollars")) {
                                updateTermFreq(currToken + " Dollars");
                                tokenIndex += 2;
                                continue;
                            }
                            // case like 500 3/4 Dollars
                            else if ((tokenIndex + 2 < sizeOfDoc) && (isNumeric(textOfDoc[tokenIndex + 1])) && textOfDoc[tokenIndex + 2].equals("Dollars")) {
                                updateTermFreq(currToken + " " + textOfDoc[tokenIndex + 1] + " Dollars");
                                tokenIndex += 3;
                                continue;
                            }
                            // case like 100 million or 100 billion or 100 trillion
                            else if ((isNextTokensDollars(textOfDoc, tokenIndex + 1, sizeOfDoc)) &&
                                    (tokenIndex + 1 < sizeOfDoc) && isMilOrBilOrTril(textOfDoc[tokenIndex + 1])) {
                                // 100 million (with or without U.S.) case
                                if (textOfDoc[tokenIndex + 1].equals("million") || textOfDoc[tokenIndex + 1].equals("m")) {
                                    // 10 million us dollars case
                                    if ((tokenIndex + 2 < sizeOfDoc) && (textOfDoc[tokenIndex + 2].equals("U.S."))) {
                                        updateTermFreq(currToken + " M Dollars");
                                        tokenIndex += 4;
                                        continue;
                                    } else { //// 10 million dollars case
                                        updateTermFreq(currToken + " M Dollars");
                                        tokenIndex += 3;
                                        continue;
                                    }
                                }
                                // 100 billion (with or without U.S.) case
                                else if ((textOfDoc[tokenIndex + 1].equals("billion") || textOfDoc[tokenIndex + 1].equals("bn"))) {
                                    // 10 billion us dollars case
                                    if ((tokenIndex + 2 < sizeOfDoc) && (textOfDoc[tokenIndex + 2].equals("U.S."))) {
                                        updateTermFreq(currToken + "0000 M Dollars");
                                        tokenIndex += 4;
                                        continue;
                                    } else { //// 10 billion dollars case
                                        updateTermFreq(currToken + "0000 M Dollars");
                                        tokenIndex += 3;
                                        continue;
                                    }
                                }
                                /// 2 trillion (with or without U.S.) case
                                else if (textOfDoc[tokenIndex + 1].equals("trillion")) {
                                    // 10 trillion us dollars case
                                    if ((tokenIndex + 2 < sizeOfDoc) && (textOfDoc[tokenIndex + 2].equals("U.S."))) {
                                        updateTermFreq(currToken + "000000 M Dollars");
                                        tokenIndex += 4;
                                        continue;
                                    } else { //// 10 trillion dollars case
                                        updateTermFreq(currToken + "000000 M Dollars");
                                        tokenIndex += 3;
                                        continue;
                                    }
                                }
                            }
                            // if number is between 1M to 1B
                        } else if((checkSizeOfNum(currToken) > 6) && ((checkSizeOfNum(currToken) < 10))){
                            //normal case, for example : 10,000,000 to 10M
                            if (!(isNextTokensDollars(textOfDoc,tokenIndex,sizeOfDoc))) {
                                float largeNum = stringToFloat(currToken);
                                updateTermFreq(largeNum / 1000000 + "M");
                                tokenIndex++;
                                continue;
                            }
                            //dollar case
                            if ((tokenIndex + 1 < sizeOfDoc) && textOfDoc[tokenIndex + 1].equals("Dollars")) {
                                float largerNum = stringToFloat(currToken);
                                updateTermFreq(largerNum / 1000000 + " M Dollars");
                                tokenIndex += 2;
                                continue;
                            }
                        }
                        // over Billion case
                        else if (checkSizeOfNum(currToken) > 10) {
                            if (!(isNextTokensDollars(textOfDoc,tokenIndex,sizeOfDoc))) {
                                currToken = removeCommaFromString(currToken);
                                if((currToken.length() > 9) && (!currToken.equals(""))){//can not convert the num, do it manually
                                    String leftSide = currToken.substring(0,currToken.length() - 9);
                                    String rightSide = currToken.substring(currToken.length() - 9, currToken.length() - 6);
                                    String largeFloat = leftSide + "." + rightSide + "B";
                                    updateTermFreq(largeFloat);
                                }
                                else {//converted num
                                    long largeNum = stringToLong(currToken);
                                    largeNum = largeNum / 1000;
                                    float convertedFloat = (float) largeNum / 1000;
                                    updateTermFreq((convertedFloat / 1000) + "B");
                                }
                                tokenIndex++;
                                continue;
                            }
                        }
                        else {
                            updateTermFreq(currToken);
                            tokenIndex++;
                            continue;
                        }

                    }
                    else if (hasTypeOfDate(currToken)){ // 4th of july case
                        if (((tokenIndex + 2 < sizeOfDoc)) && (textOfDoc[tokenIndex + 1].equals("of"))
                                && (isDate(textOfDoc[tokenIndex + 2]))) { // checking that the next tokens is " of MONTH "
                            updateTermFreq(monthes.get(textOfDoc[tokenIndex + 2]) + "-" + currToken.substring(0,currToken.length()-2));
                            tokenIndex += 3;
                            continue;
                        }
                    }
                } // end of only numeric case

                else if (currToken.charAt(0) == '$') { // if the first char is $ ---> like $50
                    // if the amount is under 1 million
                    if (stringToDouble(removeCommaFromString(currToken.substring(1,currToken.length()))) < 1000000) {
                        if (tokenIndex + 1 < sizeOfDoc) {
                            if (!(isMilOrBilOrTril(textOfDoc[tokenIndex + 1]))) { // if the next token is not mill / bill / trill
                                updateTermFreq(currToken.substring(1,currToken.length()) + " Dollars");
                                tokenIndex += 1;
                                continue;

                                /// $100 million
                            } else if (textOfDoc[tokenIndex + 1].equals("million") ||textOfDoc[tokenIndex + 1].equals("m")){
                                ////// if the next token is U.S. , ignore it and add 1 to tokenIndex
                                if (tokenIndex + 2 < sizeOfDoc && textOfDoc[tokenIndex + 2].equals("U.S.")) {
                                    updateTermFreq(currToken.substring(1,currToken.length()) + " M Dollars");
                                    tokenIndex += 4;
                                    continue;
                                }
                                else { /// if U.S. doesn't appears
                                    updateTermFreq(currToken.substring(1, currToken.length()) + " M Dollars");
                                    tokenIndex += 2;
                                    continue;
                                }

                                /// $100 billion
                            } else if (textOfDoc[tokenIndex + 1].equals("billion") ||textOfDoc[tokenIndex + 1].equals("bn")){
                                ////// if the next token is U.S. , ignore it and add 1 to tokenIndex
                                if (tokenIndex + 2 < sizeOfDoc && textOfDoc[tokenIndex + 2].equals("U.S.")) {
                                    long largeNum = stringToLong(currToken.substring(1, currToken.length())) * 1000;
                                    updateTermFreq(largeNum + " M Dollars");
                                    tokenIndex += 4;
                                    continue;
                                }
                                else { //// if U.S. doesn't appears
                                    long largeNum = stringToLong(currToken.substring(1, currToken.length())) * 1000;
                                    updateTermFreq(largeNum + " M Dollars");
                                    tokenIndex += 2;
                                    continue;
                                }
                            }
                            // $7 trillion
                            else if (textOfDoc[tokenIndex + 1].equals("trillion")) {
                                ////// if the next token is U.S. , ignore it and add 1 to tokenIndex
                                if (tokenIndex + 2 < sizeOfDoc && textOfDoc[tokenIndex + 2].equals("U.S.")) {
                                    float largeNum = stringToFloat(currToken.substring(1, currToken.length())) * 1000000;
                                    updateTermFreq(largeNum + " M Dollars");
                                    tokenIndex += 4;
                                    continue;
                                }
                                else { //// if U.S. doesn't appears
                                    float largeNum = stringToFloat(currToken.substring(1, currToken.length())) * 1000000;
                                    updateTermFreq(largeNum + " M Dollars");
                                    tokenIndex += 2;
                                    continue;
                                }
                            }


                        }

                    }
                    else { // like $250000000 ---> 250 M Dollars
                        float largerNum = stringToFloat(currToken.substring(1,currToken.length()));
                        updateTermFreq(largerNum / 1000000 + " M Dollars");
                        tokenIndex += 3;
                        continue;

                    }
                }
                /// regular term
                updateTermFreq(currToken);
                tokenIndex++;
                continue;

            }
            tokenIndex++;
            continue;
        } // end of while



        Map.Entry<String, Pair<Integer,String>> maxEntry = null;

        //will check the max tf just if there are terms for this doc.
        if(tfForDoc.size() > 0) {
            //this loop is checking the <MaxF,MaxT> in tfForDoc Map
            for (Map.Entry<String, Pair<Integer,String>> entry : tfForDoc.entrySet()) {
                if ((maxEntry == null) || (entry.getValue().getKey() > maxEntry.getValue().getKey())) {
                    maxEntry = entry;
                }
            }

            //adding cuurent Document info to allMapDoc
            Document currDoc = processor.readFile.allDocs_Map.get(docName);
            currDoc.setMax_tf(maxEntry.getValue().getKey());
            currDoc.setMostFreqTerm(maxEntry.getKey());
            currDoc.setPlacesOfMaxTF(maxEntry.getValue().getValue());
            currDoc.setNumOfTerms(numOfTerms);

        }
        else {//if there is no term for this doc, delete this doc we don't need it
            processor.readFile.allDocs_Map.remove(docName);
            processor.readFile.docsForIteration.remove(docName);
        }



        //--------------------
        //-------Terms--------
        //--add to posting----
        //--------------------

        Pair<Integer,String> tfAndPlaces;
        //adding the doc and term to the lines to posting MAP
        for (String termInDoc : tfForDoc.keySet()) {
            tfAndPlaces = tfForDoc.get(termInDoc);
            if (linesToPosting.containsKey(termInDoc))//if there is entry to this term, add the doc to the posting line of this term
                //term : doc1 3 6!205!544,doc2 1 6,doc19 2...
                linesToPosting.replace(termInDoc,linesToPosting.get(termInDoc).append("," + docName + " " + tfAndPlaces.getKey() + " " + tfAndPlaces.getValue()));
            else
                linesToPosting.put(termInDoc, new StringBuilder(docName + " " + tfAndPlaces.getKey() + " " + tfAndPlaces.getValue()));
        }


        //------------------------
        //-------Countries--------
        //-----add to posting-----
        //------------------------
        //taking the country name of this doc
        Document docToPosting = processor.readFile.allDocs_Map.get(docName);
        tfAndPlaces = null;
        //check if the doc wasn't removed from the map
        if(docToPosting != null) {
            String countryForThisDoc = ParseCountryForDoc(docToPosting.getCountry());

            if((!countryForThisDoc.equals("")) && (!countryForThisDoc.equals(" "))) {
                int tf_ToCountry = 0;
                String placesInDoc = "";
                //search for this country in the map:
                if (tfForDoc.containsKey(countryForThisDoc)) {
                    tfAndPlaces = tfForDoc.get(countryForThisDoc);
                } else if (tfForDoc.containsKey(countryForThisDoc.toLowerCase())) {
                    tfAndPlaces = tfForDoc.get(countryForThisDoc.toLowerCase());
                }

                //if we found this country in the tf map:
                if (tfAndPlaces != null) {
                    tf_ToCountry = tfAndPlaces.getKey();
                    placesInDoc = tfAndPlaces.getValue();
                }




                //if there is entry to this country, add the doc to the posting line of this country
                if (lineToPosting_Countries.containsKey(countryForThisDoc))
                    //country : doc1 3 2!565!992,doc2 1 654,doc19 2...
                    lineToPosting_Countries.replace(countryForThisDoc, lineToPosting_Countries.get(countryForThisDoc).append("," + docName + " " + tf_ToCountry + " " + placesInDoc));
                else
                    lineToPosting_Countries.put(countryForThisDoc, new StringBuilder(countryForThisDoc + ": " + docName + " " + tf_ToCountry + " " + placesInDoc));
            }
        }



        //cleaning the map of the docs and the tf in this iteration
        tfForDoc = null;
    }
    //this function parse a country from the tag <f p=104>
    private String ParseCountryForDoc(String country) {
        //check if there is country in this doc
        if ((country != null) && (!country.equals(""))) {
            if (isCountry(country)) { // if it's a country type
                country = removeUnnecessaryChars(country);
                if (!country.equals("")) {
                    if (country.contains("/")) { //if there is slash in the name
                        country = country.substring(0, country.indexOf('/'));
                        if (country.equals("The") || country.equals("the")) {
                            country = "";
                        }
                    }
                }
            }
            else {//not a country type
                country = "";
            }
        }
        return country;
    }
    //this function fix the lower and upper cases of the term in the maps
    //if we saw term with upper & lower case -> should be lower case
    private void updateLowerUpperCases(String currToken) {
        // if we need to do stem
        if (doStemming) {
            Stemmer stemmer = new Stemmer();
            stemmer.add(currToken.toCharArray(),currToken.length());
            stemmer.stem();
            currToken = stemmer.toString();
        }

        if(!currToken.equals("")) {
            //update the all terms map:
            allTerms_Map.remove(toCapitalLetters(currToken));
            allTerms_Map.put(currToken.toLowerCase(), new TermInfo(currToken.toLowerCase(),-1,-1));

            //update the term Frequency map:
            if (termFrequency.containsKey(currToken.toUpperCase())) {
                int freq = termFrequency.remove(currToken.toUpperCase());
                termFrequency.put(currToken.toLowerCase(), freq + 1);
            }

            //update the tf for doc map:
            if (tfForDoc.containsKey(currToken.toUpperCase())) { // if term already exists on map
                Pair<Integer, String> tfAndPlaces = tfForDoc.remove(currToken.toUpperCase());
                tfForDoc.put(currToken.toLowerCase(), new Pair<>(tfAndPlaces.getKey() + 1, tfAndPlaces.getValue() + "!" + placeInDoc));
            }

            //update the line to posting map:
            if (linesToPosting.containsKey(currToken.toUpperCase())) {
                StringBuilder docsForTerm = linesToPosting.remove(currToken.toUpperCase());
                linesToPosting.put(currToken.toLowerCase(), docsForTerm);
            }
        }

        //update the place in doc index
        placeInDoc++;
    }
    //--------------------------------------
    //---Insert to maps and do stemming-----
    //--------------------------------------
    // this function will get term and add it to termFreq and tfForDoc Maps
    private void updateTermFreq (String termToadd) {

        termToadd = removeUnnecessaryChars(termToadd);
        //System.out.println(termToadd);

        if (doStemming) { // if we need to do stem
            Stemmer stemmer = new Stemmer();
            stemmer.add(termToadd.toCharArray(),termToadd.length());
            stemmer.stem();
            termToadd = stemmer.toString();
        }

        //add to all terms map:
        if (!allTerms_Map.containsKey(termToadd))
            allTerms_Map.put(termToadd,new TermInfo(termToadd,-1,-1));

        // if term already exists, update his tf in the map
        if (termFrequency.containsKey(termToadd)){
            termFrequency.replace(termToadd,termFrequency.get(termToadd) + 1);
        }
        else { // if term doesn't exists on Map, add it
            termFrequency.put(termToadd,1);
            numOfTerms++;
        }

        //add to tfOfDoc MAP
        if (tfForDoc.containsKey(termToadd)){ // if term already exists on map
            Pair<Integer,String> tfAndPlaces = tfForDoc.get(termToadd);
            tfForDoc.replace(termToadd,new Pair<>(tfAndPlaces.getKey() + 1,tfAndPlaces.getValue() + "!" + placeInDoc));
        }
        else { // if term doesn't exists on Map
            tfForDoc.put(termToadd,new Pair<>(1 , "" + placeInDoc));
        }

        //update the place in doc
        placeInDoc++;
    }
    //this function will check if the last two characters of currToken is : st/nd/th/rd
    private boolean hasTypeOfDate(String currToken) {
        if (currToken.length() > 2) {
            int endOfNum = 0;
            for (int i = 0; i < currToken.length() ; i ++) { // this loop will get the last index of digit in currToken
                if (!(Character.isDigit(currToken.charAt(i)))){
                    endOfNum = i;
                    break;
                }
            }
            if (endOfNum != 0 && ((currToken.substring(endOfNum,currToken.length()).equals("st"))
                    || (currToken.substring(endOfNum,currToken.length()).equals("nd"))
                    || (currToken.substring(endOfNum,currToken.length()).equals("th"))
                    || (currToken.substring(endOfNum,currToken.length()).equals("rd")))) {
                return true;
            }

        }

        return false;
    }
    //this function check if this token have more than one point
    private boolean hasMoreThanOnePoint(String curr){
        int count = 0;
        for (int i = 0;i < curr.length(); i++) {
            if (curr.charAt(i) == '.')
                count++;
        }
        return count >= 2;
    }
    //this func. remove Unnecessary characters from the token
    private String removeUnnecessaryChars(String currToken) {
        //run while there are Unnecessary characters at the start of the token
        //or at the end of the token.
        while ( !(currToken.equals("")) && ((deleteList.contains(currToken.charAt(0))) ||
                (deleteList.contains(currToken.charAt(currToken.length() - 1))))){
            if( !(currToken.equals("")) && (deleteList.contains(currToken.charAt(0))))
                currToken = currToken.substring(1,currToken.length());
            if( !(currToken.equals("")) && (deleteList.contains(currToken.charAt(currToken.length() - 1))))
                currToken = currToken.substring(0,currToken.length() - 1);
        }
        return currToken.replace("�", "");
    }
    private boolean hasPointAtEnd (String str) {
        if (str.equals("")){
            return false;
        }
        return str.charAt(str.length()-1) == '.';
    }
    //this function takes the text and split it into tokens
    //the delimiters are blank space and enter
    private String[] getTokensFromText(String text) {
        return text.split("[,{}() -- :| \n ]");
    }
}