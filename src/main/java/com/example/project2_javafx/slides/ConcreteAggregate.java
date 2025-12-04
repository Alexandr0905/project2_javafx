package com.example.project2_javafx.slides;

import com.example.project2_javafx.model.DefaultSlide;

import java.util.ArrayList;
import java.util.List;

public class ConcreteAggregate implements SlideAggregate {

    private final List<DefaultSlide> slides;

    public ConcreteAggregate(List<DefaultSlide> slides) {
        this.slides = slides == null ? new ArrayList<>() : new ArrayList<>(slides);
    }

    @Override
    public SlideIterator getIterator() {
        return new SlideIteratorImpl();
    }

    @Override
    public int size() {
        return slides.size();
    }

    private class SlideIteratorImpl implements SlideIterator {
        private int current = -1;

        @Override
        public boolean hasNext(int x) {
            if (slides.isEmpty()) return false;
            int idx = current + x;
            // нормализуем
            int n = slides.size();
            int normalized = ((idx % n) + n) % n;
            return slides.get(normalized) != null;
        }

        @Override
        public DefaultSlide next() {
            if (slides.isEmpty()) return null;
            current = (current + 1) % slides.size();
            return slides.get(current);
        }

        @Override
        public DefaultSlide previous() {
            if (slides.isEmpty()) return null;
            current = (current - 1) % slides.size();
            if (current < 0) current += slides.size();
            return slides.get(current);
        }
    }
}