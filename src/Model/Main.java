package Model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class Main extends Application{

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    //opening fxml file and start program
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("BI-Search");
        primaryStage.setResizable(false);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/View/BI_View.fxml"));
        Scene scene = new Scene(root, 850, 510);
        primaryStage.setScene(scene);
        root.setStyle("-fx-background-color: white");
        primaryStage.show();
    }
}
