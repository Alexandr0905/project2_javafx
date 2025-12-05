package com.example.project2_javafx.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefaultSlide implements Slide {
    private final String id;
    private final File imageFile;
    private final List<String> notes = new ArrayList<>();

    public DefaultSlide(File imageFile) {
        this.id = UUID.randomUUID().toString();
        this.imageFile = imageFile;
    }

    public DefaultSlide(String id, File imageFile) {
        this.id = id;
        this.imageFile = imageFile;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public File getImageFile() {
        return imageFile;
    }

    @Override
    public List<String> getNotes() {
        return notes;
    }

    @Override
    public void addNote(String note) {
        if (note == null || note.trim().isEmpty()) return;
        notes.add(note.trim());
    }

    @Override
    public void clearNotes() {
        notes.clear();
    }

    @Override
    public String toString() {
        return imageFile != null ? imageFile.getName() : id;
    }
}