package com.example.project2_javafx.model;

import java.io.File;
import java.util.List;

public interface Slide {
    String getId();
    File getImageFile();
    List<String> getNotes();
    void addNote(String note);
    void clearNotes();
}