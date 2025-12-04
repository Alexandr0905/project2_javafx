module com.example.project2_javafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.project2_javafx to javafx.fxml;
    exports com.example.project2_javafx;
}