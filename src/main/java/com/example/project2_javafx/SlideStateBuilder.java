package com.example.project2_javafx;

public class SlideStateBuilder {

    private final int current;
    private final int total;

    private SlideStateBuilder(Builder builder) {
        this.current = builder.current;
        this.total = builder.total;
    }

    public String getText() {
        return String.format("Слайд: %d из %d", current, total);
    }

    public static class Builder {
        private int current;
        private int total;

        public Builder current(int value) {
            this.current = value;
            return this;
        }

        public Builder total(int value) {
            this.total = value;
            return this;
        }

        public SlideStateBuilder build() {
            return new SlideStateBuilder(this);
        }
    }
}