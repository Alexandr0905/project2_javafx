package com.example.project2_javafx.service;

import com.example.project2_javafx.model.Slide;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import javax.imageio.ImageIO;
import java.io.File;

public class SlideRenderer {

    /**
     * Сохраняет слайд (изображение + заметки) в PNG.
     * @param slide - объект Slide
     * @param outFile - файл, куда сохраняем
     */
    public static void renderToFile(Slide slide, File outFile) {
        if (slide == null || slide.getImageFile() == null || outFile == null) return;

        try {
            // загружаем исходное изображение
            Image baseImage = new Image(slide.getImageFile().toURI().toString());

            // создаём Canvas с размерами изображения
            Canvas canvas = new Canvas(baseImage.getWidth(), baseImage.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // рисуем изображение
            gc.drawImage(baseImage, 0, 0);

            // рисуем заметки по умолчанию снизу
            gc.setFill(Color.color(0, 0, 0, 0.6)); // полупрозрачный фон для текста
            gc.fillRect(0, baseImage.getHeight() - 30 * slide.getNotes().size(), baseImage.getWidth(), 30 * slide.getNotes().size());

            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 20));
            double y = baseImage.getHeight() - 30 * slide.getNotes().size() + 22;
            for (String note : slide.getNotes()) {
                gc.fillText(note, 10, y);
                y += 30;
            }

            // snapshot Canvas
            javafx.scene.image.WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);

            // сохраняем в файл
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", outFile);

            System.out.println("Слайд сохранён: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}