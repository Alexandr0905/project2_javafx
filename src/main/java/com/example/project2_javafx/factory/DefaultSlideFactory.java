package com.example.project2_javafx.factory;

import com.example.project2_javafx.model.DefaultSlide;
import com.example.project2_javafx.service.AnimationSpec;

import java.io.File;
import java.util.List;

public class DefaultSlideFactory implements SlideFactory {

    @Override
    public DefaultSlide createImageSlide(File imageFile) {
        return new DefaultSlide(imageFile);
    }

    @Override
    public DefaultSlide createSlideWithNotes(File imageFile, List<String> notes) {
        DefaultSlide slide = new DefaultSlide(imageFile);
        if (notes != null) {
            for (String n : notes) slide.addNote(n);
        }
        return slide;
    }

    @Override
    public DefaultSlide createAnimatedSlide(File imageFile, AnimationSpec animationSpec) {
        DefaultSlide slide = new DefaultSlide(imageFile);
        return slide;
    }
}