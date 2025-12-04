package com.example.project2_javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1246, 573);
        stage.setTitle("Альбом любимых фотографий");
        stage.setScene(scene);
        stage.show();
        System.out.println(getClass().getResource("/com/example/project2_javafx/image/v1.png"));
    }
}
