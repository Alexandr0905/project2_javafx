package com.example.project2_javafx.slides;

import com.example.project2_javafx.model.DefaultSlide;

public interface SlideIterator {
    boolean hasNext(int x);
    DefaultSlide next();
    DefaultSlide previous();
}