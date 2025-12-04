package com.example.project2_javafx.factory;

import com.example.project2_javafx.model.DefaultSlide;
import com.example.project2_javafx.service.AnimationSpec;

import java.io.File;
import java.util.List;

public interface SlideFactory {
    DefaultSlide createImageSlide(File imageFile);
    DefaultSlide createSlideWithNotes(File imageFile, List<String> notes);
    DefaultSlide createAnimatedSlide(File imageFile, AnimationSpec animationSpec);
}