package com.example.project2_javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.*;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick()
    {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
