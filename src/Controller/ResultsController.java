package Controller;

import Model.Processor;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static javafx.collections.FXCollections.observableArrayList;


public class ResultsController {

    public Button btn_chooseDoc;
    public BorderPane bp_pane;
    public Button btn_save;
    public Button btn_browse;
    public TextField tf_savingPath;
    private ListView<String> lv_listView;

    private Processor processor;
    private static int resultNum = 1; //static member for the number on the current file to write

    // this function will get the list of top 5 terms in doc and show it to user
    public void ShowFiveMostQForDoc() {

        ListView<String> list = new ListView<>();
        ObservableList<String> items = observableArrayList();
        List<Pair<String, Float>> dict;
        if (processor != null) { // if there is something to show
            // take the map from proccesor
            dict = processor.getTop5terms();

            Iterator it = dict.iterator();
            while (it.hasNext()) {
                Pair <String,Integer> curr = (Pair <String,Integer>)it.next();
                items.add("Term: " + curr.getKey() + " -> " + curr.getValue());
            }
            list.setItems(items);
            Stage stage = new Stage();
            stage.setTitle("Top 5 Terms In Doc");
            BorderPane pane = new BorderPane();
            Scene s = new Scene(pane,150,140);
            stage.setScene(s);
            stage.setResizable(false);
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
            alert.setContentText("No 5 Terms to show");
            alert.showAndWait();
        }

    }
    // this function will take the doc name from gui and send it ShowFiveMostQForDoc function
    public void takeDocNameFromGUI(ActionEvent actionEvent) {
        ReadOnlyObjectProperty doc = lv_listView.getFocusModel().focusedItemProperty();
        if(doc.getValue() != null) {
            String docName = (String) doc.getValue();

            if((!docName.contains("num of")) && (!docName.contains("Query"))) {
                processor.ParseAndFindFive(docName);
                ShowFiveMostQForDoc();
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("You must choose the doc you want to view");
                alert.showAndWait();
            }
        }else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("You must choose the doc you want to view");
            alert.showAndWait();
        }
    }
    // this function will open new file and write on him the result of user's choice
    public void WriteResultsToFile(ActionEvent actionEvent) {
        File f;
        BufferedWriter br;
        FileOutputStream output;
        try{
            f = new File(tf_savingPath.getText() + "//results" + resultNum + ".txt");
            output = new FileOutputStream(f);
            br = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            resultNum++;

            PrintWriter out = new PrintWriter(br);

            String query_id = "";
            String Docno = "";
            for (String currItem:lv_listView.getItems()) {
                //if it's new query results:
                if (currItem.contains("Query num")) {
                    query_id = currItem.substring(currItem.indexOf(":") + 2);
                    if(query_id.charAt(query_id.length() -1) == ' ')
                        query_id = query_id.substring(0, query_id.length()-1);
                }
                // if it's file for query:
                else if ((!currItem.contains("Query num")) && (!currItem.contains("num of")) &&
                        (!currItem.contains("New Query")) && (!currItem.contains("rum")) && (!currItem.contains("run"))) {
                    //writing the line to the file
                    if(currItem.contains(":")) {
                        Docno = currItem.substring(0, currItem.indexOf(":") - 1);
                        out.println(query_id + " 0 " + Docno + " 1 42.38 mt");
                    }
                    else {
                        out.println(query_id + " 0 " + currItem + " 1 42.38 mt");
                    }

                }
            }

            out.close();
            br.close();
            output.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Saved");
            alert.setContentText("Your results to this Query has been saved!");
            alert.showAndWait();

        }catch (IOException o){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("You must choose a saving path first");
            alert.showAndWait();
        }
    }
    // function for Load and Save scenes
    public void browse (ActionEvent event) {
        try {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setInitialDirectory((new File("C:\\")));
            File selectedFile = dc.showDialog(null);
            String s = selectedFile.getAbsolutePath();
            tf_savingPath.setText(s);
        } catch (Exception e) {
        }
    }
    //setter
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }
    //setter
    public void setResultList(ListView<String> resultList) {
        this.lv_listView = resultList;
    }
    //setter
    public void setResultsOnScreen(){
        bp_pane.setCenter(lv_listView);
    }


}
