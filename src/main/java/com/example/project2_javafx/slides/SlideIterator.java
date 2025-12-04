package com.example.project2_javafx.slides;

import com.example.project2_javafx.model.Slide;

public interface SlideIterator {
    boolean hasNext(int x);
    Slide next();
    Slide previous();
}