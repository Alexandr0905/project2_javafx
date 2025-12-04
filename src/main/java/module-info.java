module com.example.project2_javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;
//    requires com.example.project2_javafx;


    opens com.example.project2_javafx to javafx.fxml;
    exports com.example.project2_javafx;
    exports com.example.project2_javafx.service;
    opens com.example.project2_javafx.service to javafx.fxml;
}