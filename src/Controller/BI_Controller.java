package Controller;

import Model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.Color;
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

    private ReadFile readFile; // creating process to index all corpus
    private File savingDir; // keeping saving path dir
    //boolean vars to check legality while running
    boolean finish = false;
    boolean running = false;
    boolean cacheAndDict = false;

    private HashMap<String,TermInfo> allTerms_Map_COPY; //loaded cache of the terms map
    private HashMap<String,CountryInfo> countryMap_COPY;//loaded cache of the countries map
    private HashMap<String,Document> allDocs_Map_COPY;//loaded cache of the documents map

    @Override
    //init logo img and all languages
    public void initialize(URL location, ResourceBundle resources) {
        cb_language.setDisable(true);
        //set logo image
        setImage(img_logo,"Resources/logo.jpg");
    }

    // set all Languages
    private void setLanguages() {
        cb_language.setDisable(false);
        cb_language.setItems(FXCollections.observableArrayList(readFile.getLanguagesSet()));
        cb_language.setValue("English");
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

            // reset copy's maps
            allDocs_Map_COPY = null;
            allTerms_Map_COPY = null;
            countryMap_COPY = null;


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
                    path = tf_savingPath.getText()+"\\with stemme";
                else
                    path = tf_savingPath.getText()+"\\without stemme";
                savingDir=new File(path);
                savingDir.mkdir(); //make dir for with/wothout Stemme
                readFile = new ReadFile(tf_corpus.getText(), savingDir.getPath(), cb_stem.isSelected());
                //read the files
                readFile.readAllFiles();

                endTime = System.currentTimeMillis();

                //after the invert indexing, we can use the docs languages in the gui:
                setLanguages();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long totalTime = (endTime - startTime) / 1000;

            Processor processor = readFile.getProcessor();

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            //frame.dispose();

            //show information
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Index Information");
            alert.setContentText("# Of Documents: " + readFile.getTotalNumOfDocs() + "\n" +
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

            //delete loaded docs
            allTerms_Map_COPY = null;
            countryMap_COPY = null;
            allDocs_Map_COPY = null;

            //if there was a running index before
            if ((finish) && (readFile != null)) {
                readFile.getProcessor().parser.allTerms_Map = null;
                readFile.getProcessor().indexer.countryMap = null;
                readFile.allDocs_Map = null;
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
        if (readFile != null) { // if we want to show original map
            dict = readFile.getProcessor().parser.allTerms_Map;
        }
        else { // if we want to show loaded map
            dict = allTerms_Map_COPY;
        }
        if (dict != null) { // if dic is legal
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
        else if (tf_loadCache.getText().trim().isEmpty() || dict == null) // alert case caused by empty path or no dic at all
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("No path entered \n Or no dictionary to load");
            alert.showAndWait();
        }

    }

    //loading the maps to cache
    public void LoadCache(ActionEvent actionEvent) {
        //check if the check box of stem was selected
        if (tf_loadCache.getText().length() > 0) {
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
                frame.add(new JLabel( loading, JLabel.CENTER));

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(390, 300);
                frame.getContentPane().setBackground(Color.white);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                // end of loading bar

                ReadObjectFromFile(path);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cache uploaded");
                alert.setContentText("Cache uploaded successfully");
                alert.showAndWait();
            } else {//the file not exist
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setContentText("no cache to load");
                alert.showAndWait();
            }
        }

        else { // if loading path is empty
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("You must choose loading path");
            alert.showAndWait();
        }
    }

    //this function reads the two maps of info from the disc
    private void ReadObjectFromFile(String path) {
        boolean isReadFileInitialized = (readFile != null);

        this.allTerms_Map_COPY = new HashMap<>();
        this.countryMap_COPY = new HashMap<>();
        this.allDocs_Map_COPY = new HashMap<>();


        File toRead = new File(path + "/TermsInfoMap");
        try {
            //read the terms map
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
            if (isReadFileInitialized) {
                readFile.getProcessor().parser.allTerms_Map =  (HashMap<String, TermInfo>) ois.readObject();
            }
            else {
                allTerms_Map_COPY = (HashMap<String, TermInfo>) ois.readObject();
            }
            ois.close();
            fis.close();

            //read the countries map
            toRead = new File(path + "/CountriesInfoMap");
            fis = new FileInputStream(toRead);
            ois = new ObjectInputStream(fis);
            if (isReadFileInitialized) {
                readFile.getProcessor().indexer.countryMap = (HashMap<String, CountryInfo>) ois.readObject();
            }
            else {
                countryMap_COPY = (HashMap<String, CountryInfo>) ois.readObject();
            }
            ois.close();
            fis.close();

            //read the documents map
            toRead = new File(path + "/DocsInfoMap");
            fis = new FileInputStream(toRead);
            ois = new ObjectInputStream(fis);
            if (isReadFileInitialized) {
                readFile.allDocs_Map = (HashMap<String, Document>) ois.readObject();
            }
            else {
                allDocs_Map_COPY = (HashMap<String, Document>) ois.readObject();
            }
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

}