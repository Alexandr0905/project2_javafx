package com.example.project2_javafx.slides;

import com.example.project2_javafx.model.Slide;

public interface SlideAggregate {
    SlideIterator getIterator();
    int size();
}