package Controller;

import Model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

import static javafx.collections.FXCollections.*;

public class BI_Controller implements Initializable {
    // init all buttons,text fields,checkboxes and image view
    public ImageView img_logo;
    public Button btn_savingPath;
    public Button btn_files;
    public Button btn_start;
    public Button btn_showDictionary;
    public Button btn_resetAll;
    public Button btn_browseForCache;
    public Button btn_loadCache;
    public CheckBox cb_stem;
    public TextField tf_savingPath;
    public TextField tf_corpus;
    public TextField tf_loadCache;
    public ChoiceBox cb_language;

    // part B
    public TextField tf_freeQuery;
    public TextField tf_queriesFiles;
    public Button btn_run;
    public Button btn_browseQueriesFiles;
    public Button btn_runQueries;
    public MenuButton mb_cities;
    public CheckBox cb_semantic;
    public ResultsController resultsController;


    private Processor processor; // creating process to index all corpus
    private File savingDir; // keeping saving path dir
    private HashSet<String> descStopWords;
    //boolean vars to check legality while running
    boolean finish = false;
    boolean running = false;
    boolean cacheAndDict = false;

    @Override
    //init logo img and all languages
    public void initialize(URL location, ResourceBundle resources) {
        cb_language.setDisable(true);
        mb_cities.setDisable(true);
        //set logo image
        setImage(img_logo,"Resources/logo.jpg");
    }

    // set all Languages
    private void setLanguages() {
        try {
            cb_language.setDisable(false);
            cb_language.setItems(FXCollections.observableArrayList(processor.readFile.getLanguagesSet()));
            cb_language.setValue("English");
        }catch (Exception e){

        }
    }

    //this function will set image
    public void setImage(ImageView imageView, String filePath) {
        File file = new File(filePath);
        //Image image = new Image(this.getClass().getClassLoader().getResourceAsStream(filePath));
        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);
    }

    // opening load scene
    public void pathOfCorpus (ActionEvent event) {
        try {
            browse(event,tf_corpus);
        } catch (Exception e) {
        }

    }

    // opening load scene
    public void pathOfCache (ActionEvent event) {
        try {
            browse(event,tf_loadCache);
        } catch (Exception e) {
        }
    }

    // opening save scene
    public void savingPath (ActionEvent event) {
        try {
            browse(event,tf_savingPath);
        } catch (Exception e) {

        }
    }

    // function for Load and Save scenes
    private void browse (ActionEvent event, TextField field) {
        try {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setInitialDirectory((new File("C:\\")));
            File selectedFile = dc.showDialog(null);
            String s = selectedFile.getAbsolutePath();
            field.setText(s);
        } catch (Exception e) {
        }
    }

    // this function is for "Lets go" button
    public void start(ActionEvent actionEvent) throws IOException, InterruptedException {
        //check if input is not empty
        if(!(tf_corpus.getText().trim().isEmpty() || tf_savingPath.getText().trim().isEmpty() || running)) {


            // loading bar
            JFrame frame = new JFrame("Indexing...");
            frame.pack();

            ImageIcon loading = new ImageIcon("Resources/load.gif");
            frame.add(new JLabel( loading, JLabel.CENTER));

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(360, 300);
            frame.getContentPane().setBackground(Color.white);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // end of loading bar

            finish=false;
            running=true;

            long startTime = System.currentTimeMillis();
            long endTime = 0;
            try { // first we make dir and than read files
                String path;
                if(cb_stem.isSelected()) // if checkbox of stem is selected
                    path = tf_savingPath.getText() + "\\with stemme";
                else
                    path = tf_savingPath.getText() + "\\without stemme";
                savingDir=new File(path);
                savingDir.mkdir(); //make dir for with/wothout Stemme
                processor = new Processor(tf_corpus.getText(), savingDir.getPath(), cb_stem.isSelected());
                //read the files
                processor.readAllFiles();

                endTime = System.currentTimeMillis();

                //after the invert indexing, we can use the docs languages & countries in the GUI:
                setLanguages();
                setCountries();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            processor.parser.setIsQuery(false);

            long totalTime = (endTime - startTime) / 1000;

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            //frame.dispose();

            //show information
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Index Information");
            alert.setContentText("# Of Documents: " + processor.readFile.getTotalNumOfDocs() + "\n" +
                    "# Of Uniq Terms: " + processor.parser.allTerms_Map.size() + "\n" +
                    "# Of Uniq Countries: " + processor.indexer.countryMap.size() + "\n" +
                    "Total Running Time: " + totalTime + " seconds\n");
            alert.showAndWait();

            finish=true;
            running=false;
            cacheAndDict=true;
        }
        else //no pathes entered case
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ERROR");
            String s = "You must fill data Path and saving posting Path!";
            if(running)
                s = "Can't run while running";
            alert.setContentText(s);
            alert.showAndWait();
        }

    }

    // this function will reset all details
    public void reset(ActionEvent actionEvent) {

        //if the saving path is filled
        if(!tf_savingPath.getText().trim().isEmpty()) {
            String with_path = tf_savingPath.getText() + "\\with stemme";
            String without_path = tf_savingPath.getText() + "\\without stemme";

            savingDir = new File(with_path);
            File with_postingDir = new File(savingDir.getPath());
            savingDir = new File(without_path);
            File without_postingDir = new File(savingDir.getPath());
            //delete all dir
            deleteDirectory(with_postingDir);
            deleteDirectory(without_postingDir);

            //if there was a running index before
            if ((finish) && (processor != null)) {
                processor.parser.allTerms_Map = new HashMap<>();
                processor.indexer.countryMap = new HashMap<>();
                processor.readFile.allDocs_Map = new HashMap<>();
                cacheAndDict = false;
                finish = false;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Finish Reset");
            alert.setContentText("Posting and Memory deleted");
            alert.showAndWait();
        }else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ERROR");
            alert.setContentText("You must fill the saving posting Path!");
            alert.showAndWait();
        }

    }

    // this function will get File and it will delete all files inside, includes dirs and files
    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null != files){
                for(int i = 0; i < files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);//recursive call
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    // this function will open new scene and show the dictionary
    public void viewDictionary(ActionEvent actionEvent) {

        ListView<String> list = new ListView<>();
        ObservableList<String> items = observableArrayList();
        Map<String, TermInfo> dict;
        if (processor != null) { // if there is something to show
            dict = processor.parser.allTerms_Map;
            List<String> sortedTerms = new ArrayList<String>(dict.keySet());
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

            for (String s : sortedTerms) {
                items.add("Term: " + s + " -> " + dict.get(s).getSumTf());
            }

            list.setItems(items);

            Stage stage = new Stage();
            stage.setTitle("Dictionary");
            BorderPane pane = new BorderPane();
            Scene s = new Scene(pane);
            stage.setScene(s);
            pane.setCenter(list);
            stage.setAlwaysOnTop(true);
            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.close();
            });
            stage.showAndWait();
        }
        else // if there is no dictionary in the memory
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("No dictionary to load, \n" +
                    "Please load cache or run the index");
            alert.showAndWait();
        }

    }

    //loading the maps to cache
    public void LoadCache(ActionEvent actionEvent) {
        //check if the check box of stem was selected
        if ((tf_loadCache.getText().length() > 0) && (tf_corpus.getText().length() > 0)) {
            String path;
            if (cb_stem.isSelected())  // if checkbox of stem is selected
                path = tf_loadCache.getText() + "\\with stemme";
            else
                path = tf_loadCache.getText() + "\\without stemme";

            //check if terms file exist (if he exist so the country file exist too)
            File termsFile = new File(path);
            if (termsFile.exists() && termsFile.isDirectory() && AreMapsExistsOnDisk(path)) {
                // loading bar
                JFrame frame = new JFrame("Loading Cache");
                frame.pack();
                ImageIcon loading = new ImageIcon("Resources/load.gif");
                frame.add(new JLabel(loading, JLabel.CENTER));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(390, 300);
                frame.getContentPane().setBackground(Color.white);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                // end of loading bar

                ReadObjectFromFile(path);

                //set the languages and countries in the GUI
                setLanguages();
                setCountries();

                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cache uploaded");
                alert.setContentText("Cache uploaded successfully to memory");
                alert.showAndWait();
            } else {//the file not exist
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setContentText("No cache to load");
                alert.showAndWait();
            }
        } else { // if loading path is empty
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("You must choose loading path \n And select the dir that the corpus is in it");
            alert.showAndWait();
        }
    }


    //this function reads the two maps of info from the disc
    private void ReadObjectFromFile(String path) {
        //if we load cache and there was no running before:
        if(processor == null){
            processor = new Processor(tf_corpus.getText(),tf_loadCache.getText(),cb_stem.isSelected(),true);
        }

        File toRead = new File(path + "/TermsInfoMap");
        try {
            //read the terms map
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
            processor.parser.allTerms_Map =  (HashMap<String, TermInfo>) ois.readObject();

            ois.close();
            fis.close();

            //read the countries map
            toRead = new File(path + "/CountriesInfoMap");
            fis = new FileInputStream(toRead);
            ois = new ObjectInputStream(fis);
            processor.indexer.countryMap = (HashMap<String, CountryInfo>) ois.readObject();

            ois.close();
            fis.close();

            //read the documents map
            toRead = new File(path + "/DocsInfoMap");
            fis = new FileInputStream(toRead);
            ois = new ObjectInputStream(fis);
            processor.readFile.allDocs_Map = (HashMap<String, Document>) ois.readObject();

            ois.close();
            fis.close();

            //read the languages set
            toRead = new File(path + "/LanguagesSet");
            fis = new FileInputStream(toRead);
            ois = new ObjectInputStream(fis);
            processor.readFile.setLanguagesSet((Set<String>) ois.readObject());

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

    //this function checks if the files of the maps are in the disk
    private boolean AreMapsExistsOnDisk(String path) {
        File f1 = new File (path + "/TermsInfoMap");
        File f2 = new File(path + "/CountriesInfoMap");
        File f3 = new File(path + "/DocsInfoMap");
        return f1.exists() && f2.exists() && f3.exists();
    }




    //-------------------------------
    //----------Part-----------------
    //-----------B-------------------
    //-------------------------------

    // load path of queries files
    public void browseQueries(ActionEvent event) {
        try
        {
            FileChooser dc=new FileChooser();
            dc.setInitialDirectory((new File("C:\\")));
            File selectedFile=dc.showOpenDialog(null);
            String s=selectedFile.getAbsolutePath();
            tf_queriesFiles.setText(s);
        }
        catch (Exception e)
        {

        }
    }
    // this function will take care of one query case
    public void simpleQuery(){
        //if there is terms to work with:
        if((processor != null)) {
            String query = tf_freeQuery.getText();
            if(!query.equals("")) {//if the query is not empty
                processor.parser.setIsQuery(true);
                processor.parser.queryTermsMap = new HashMap<>();//reset the map

                String[] splittedQuery = query.split(" ");

                // if user choose semantic, add result to query
                if (cb_semantic.isSelected()) {
                    String semantic = addSemanticWords(query);
                    query = query + " " + semantic;
                }

                Searcher searcher = new Searcher(query,splittedQuery.length, processor, cb_stem.isSelected(),cb_semantic.isSelected(),getCheckedCountries());
                ObservableList<String> items = FXCollections.observableArrayList();

                // generate a random integer from 100 to 999
                Random random = new Random();
                int queryNum = random.nextInt(900) + 100;

                items.add("Query number: " + queryNum);
                items.add("num of docs : " + searcher.resultList.size());
                for (Map.Entry<String, Float> s : searcher.resultList) {

                    items.add(s.getKey());
                }
                ListView<String> list = new ListView<>();
                list.setItems(items);


                showResultsOnScreen(list);

            }else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("You must write a query first!");
                alert.showAndWait();
            }
        }else {//there is nothing to work with
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("You must run the index first \n " +
                    "Or load the memory from the disk!");
            alert.showAndWait();
        }
    }
    // this function will take care of many queries case
    public void manyQueries() throws IOException {
        if ((processor != null)) { // if processor is exists
            processor.parser.setIsQuery(true);
            processor.parser.queryTermsMap = new HashMap<>();//reset the map

            long startTime = System.currentTimeMillis();
            //path of file with queries
            String path = tf_queriesFiles.getText();
            if (path.length() > 0) { // if path is not empty
                File f = new File(path);
                if (f.exists()) { // if file is legal
                    FileInputStream file = new FileInputStream(path);
                    Map<String, String> queries = new TreeMap<>();
                    Map<String,StringBuilder> desc = new TreeMap<>();
                    BufferedReader br = new BufferedReader(new InputStreamReader(file));

                    String line = "";
                    String numQuery = "";
                    while ((line = br.readLine()) != null) {
                        if (line.contains("<num>")) {
                            numQuery = line.substring(line.indexOf(':') + 2);
                            if (numQuery.charAt(numQuery.length()-1) == ' '){ // if last char in numQuery is " " remove it
                                numQuery = numQuery.substring(0,numQuery.length()-1);
                            }
                            line = br.readLine();
                            String query = line.substring(line.indexOf('>') + 2);
                            queries.put(numQuery, query);
                        }
                        if (line.contains("<desc>")) {
                            // while line is not empty and we didn't got to next flag
                            StringBuilder descOfQuery = new StringBuilder();
                            while (((line = br.readLine()).length() > 0) && (!(line.contains("<")))) {
                                descOfQuery.append(line).append("\n");

                            }
                            desc.put(numQuery,descOfQuery);
                        }

                    }
                    Searcher searcher;
                    ObservableList<String> items = FXCollections.observableArrayList();
                    for (String queryNum : queries.keySet()) {

                        String query = queries.get(queryNum);
                        String[] splittedQuery = query.split(" ");

                        // if user choose semantic, add result to query
                        if (cb_semantic.isSelected()) {
                            String semantic = addSemanticWords(query);
                            query = query + " " + semantic;
                        }

                        String description = desc.get(queryNum).toString();
                        String[] descArr = description.split(" ");
                        if ((descStopWords == null) || descStopWords.isEmpty()) { // if map of stop words (of desc) is empty, init it
                            initDescStopWords();
                        }

                        // remove all stop words of desc
                        String desWithoutStopWords = "";
                        for (int i = 0 ; i < descArr.length ; i ++) {
                            if (!(descStopWords.contains(descArr[i].toLowerCase()))){
                                desWithoutStopWords += descArr[i] + " ";
                            }
                        }

                        query = query + " " + desWithoutStopWords;

                        query = query.replace("\n", "");

                        searcher = new Searcher(query,splittedQuery.length, processor, cb_stem.isSelected(), true,getCheckedCountries());
                        items.add("Query number: " + queryNum);
                        items.add("num of docs: " + searcher.resultList.size());

                        for (Map.Entry<String, Float> s : searcher.resultList) {
                            items.add(s.getKey());
                        }
                        items.add("---------New Query---------");
                    }

                    long endTime = System.currentTimeMillis();
                    long totalTime = (endTime - startTime) / 1000;
                    items.add("rum time in seconds:" + totalTime);

                    ListView<String> list = new ListView<>();
                    list.setItems(items);

                    showResultsOnScreen(list);

                    // close files
                    file.close();
                    br.close();

                }
                else { // if file is not legal
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Chosen file is illegal");
                    alert.showAndWait();
                }
            }
            else { // if path is empty
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("You must insert the path of queries.txt file !");
                alert.showAndWait();
            }

        }else {//if there is no memory or inverted index loaded:
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("You must run the index first \n " +
                    "Or load the memory from the disk!");
            alert.showAndWait();
        }


    }
    // init stop words (of description) list
    private void initDescStopWords() {
        descStopWords = new HashSet<>();
        descStopWords.add("information");
        descStopWords.add("available");
        descStopWords.add("impact");
        descStopWords.add("role");
        descStopWords.add("identify");
        descStopWords.add("instance");
        descStopWords.add("instances");
        descStopWords.add("documents");
        descStopWords.add("document");
        descStopWords.add("discuss");
        descStopWords.add("background");
        descStopWords.add("current");
        descStopWords.add("i.e.");
        descStopWords.add("something");
        descStopWords.add("effective");
        descStopWords.add("only");
        descStopWords.add("other");
        descStopWords.add("use");
        descStopWords.add("associated");
        descStopWords.add("associate");
        descStopWords.add("what");
        descStopWords.add("is");
        descStopWords.add("on");
        descStopWords.add("the");
        descStopWords.add("if");
        descStopWords.add("a");
        descStopWords.add("when");




    }
    // this function will open new scene and will show there the results of the query
    public void showResultsOnScreen(ListView<String> resultList) {
        try {
            //openning new Stage to show in
            Stage stage = new Stage();
            stage.setTitle("Your Results");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("/View/Results.fxml").openStream());
            root.setStyle("-fx-background-color: white");

            Scene scene = new Scene(root, 550, 400);
            stage.setScene(scene);

            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.close();
            });

            //loading the controllers of the new stage:
            resultsController = fxmlLoader.getController();
            //((Model)model).addObserver(updateController);

            //updateController.setModel(model);
            resultsController.setProcessor(processor);
            resultsController.setResultList(resultList);


            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes

            resultsController.setResultsOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // set the countries inside the combo box in the GUI
    public void setCountries() {
        mb_cities.setDisable(false);
        Set<String> allCities = processor.indexer.countryMap.keySet();
        for (String city : allCities) {
            CheckMenuItem menuItem = new CheckMenuItem(city);
            mb_cities.getItems().add(menuItem);
        }
    }
    // get the countries that checked in the GUI
    public Set<String> getCheckedCountries() {
        Set<String> ans = new HashSet<>();
        for (MenuItem item : mb_cities.getItems()){
            CheckMenuItem tmp = (CheckMenuItem) item;
            if (tmp.isSelected()){
                ans.add(tmp.getText());
            }
        }
        return ans;
    }
    // this function will return all results from semantic object
    private String addSemanticWords(String query) {
        String ans = "";
        String[] arr = query.split(" ");
        for (int i = 0 ; i < arr.length ; i ++) {
            Semantic sem = new Semantic(arr[i]);
            HashSet<String> set = sem.runSemantic(3);
            Iterator it = set.iterator();
            while (it.hasNext()) {
                ans += it.next()+ " ";
            }
        }
        return ans;
    }
}