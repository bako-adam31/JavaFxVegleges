package org.example.aaafcoiprojekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        Font.loadFont(getClass().getResource("fonts/NotoColorEmoji-Regular.ttf").toExternalForm(), 16);


        String cssPath = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        stage.setFullScreen(true);

        stage.setTitle("WORLD CUP SIMULATOR");
        stage.setScene(scene);
        stage.show();
    }
}
