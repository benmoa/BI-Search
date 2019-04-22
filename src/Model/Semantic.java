package Model;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashSet;

public class Semantic {
	// source word
    public String word;
	
	// C'tor
    public Semantic (String word){
        this.word = word;
    }

	// run semantic function and return HashSet(with given size) that contains simmilar words
    public HashSet<String> runSemantic(int size) {
        HashSet<String> allSemantic = new HashSet<>();
        String url = "http://api.datamuse.com/words?rel_syn=" + word;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        String data = "";
        try {
            data = client.newCall(request).execute().body().string();
            if(data != null && data.length() > 2) {
                String cleadData = removeUnnecData(data);
                String[] splittedData = cleadData.split("}");
                int i = 0;
                while ((i < splittedData.length) && (i < size)) {
                    int start = getFirstIndexOf(splittedData[i],':') + 2;
                    int stop = getStopIndex(splittedData[i]);
                    allSemantic.add(splittedData[i].substring(start,stop));
                    i++;
                }

            }
        } catch (IOException e) {
        }

        return allSemantic;
    }

    private String removeUnnecData(String data) {
        String ans = "";
        for (int i = 0 ; i < data.length() ; i ++) {
            if ((data.charAt(i) != '[') && ((data.charAt(i) != ']'))){
                ans += data.charAt(i);
            }
        }
        return ans;
    }

    // this function will return that index of first char given
    private int getFirstIndexOf(String splittedData, char c) {
        int ans = 0 ;
        for (int i = 0 ; i < splittedData.length() ; i ++) {
            if (splittedData.charAt(i) == c) {
                ans = i ;
                break;
            }
        }
        return ans;
    }
	// this function will return index of last " in given string
    private int getStopIndex(String splittedData) {
        int ans = 0;
        int counter = 0;
        for( int i = 0 ; i < splittedData.length() ; i ++) {
            if (splittedData.charAt(i) == '"') {
                counter++;
                if (counter == 4) {
                    ans = i ;
                    break;
                }
            }
        }
        return ans;
    }


}
