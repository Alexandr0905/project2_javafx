package com.example.project2_javafx.service;

import com.example.project2_javafx.factory.SlideFactory;
import com.example.project2_javafx.model.DefaultSlide;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ProjectZipIO
{
    // сохранение проекта
    public static void saveProject(File outZipFile,
                                   List<DefaultSlide> slides,
                                   AnimationType animationType,
                                   long animationSpeedMillis) throws Exception {

        Path tempDir = Files.createTempDirectory("myshow_project_");

        // 1) META
        File meta = new File(tempDir.toFile(), "meta.txt");
        try (PrintWriter pw = new PrintWriter(meta)) {
            pw.println("slideCount=" + slides.size());
            pw.println("animationType=" + (animationType != null ? animationType.name() : "FADE"));
            pw.println("animationSpeed=" + animationSpeedMillis);
        }

        // 2) SLIDES and NOTES
        File slidesDir = new File(tempDir.toFile(), "slides");
        slidesDir.mkdirs();
        File notesDir = new File(tempDir.toFile(), "notes");
        notesDir.mkdirs();

        for (int i = 0; i < slides.size(); i++) {
            DefaultSlide slide = slides.get(i);
            String imgName = String.format("slide_%04d.png", i + 1);

            // copy image
            Files.copy(slide.getImageFile().toPath(),
                    new File(slidesDir, imgName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            // save notes
            File noteFile = new File(notesDir, imgName.replace(".png", ".txt"));
            try (PrintWriter npw = new PrintWriter(noteFile)) {
                for (String note : slide.getNotes()) npw.println(note);
            }
        }

        // 3) zip the tempDir into outZipFile
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZipFile))) {
            Path base = tempDir;
            Files.walk(base)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(path -> {
                        try {
                            String entryName = base.relativize(path).toString().replace("\\", "/");
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (Exception ignored) { }
                    });
        }

        // 4) cleanup
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> p.toFile().delete());
    }

    public record LoadedProject(List<DefaultSlide> slides,
                                AnimationType animationType,
                                long animationSpeed) {}

    // загрузка проекта
    public static LoadedProject loadProject(File zipFile, SlideFactory factory) throws Exception {

        Path tempDir = Files.createTempDirectory("myshow_load_");

        unzip(zipFile, tempDir.toFile());

        File meta = new File(tempDir.toFile(), "meta.txt");
        List<String> lines = Files.readAllLines(meta.toPath());

        // --- Читаем количество слайдов
        int slideCount = 0;
        AnimationType loadedAnimationType = AnimationType.FADE; // дефолт
        long loadedAnimationSpeed = 400; // дефолт

        for (String line : lines) {
            if (line.startsWith("slideCount=")) {
                slideCount = Integer.parseInt(line.split("=")[1].trim());
            }
            if (line.startsWith("animationType=")) {
                loadedAnimationType = AnimationType.valueOf(line.split("=")[1].trim());
            }
            if (line.startsWith("animationSpeed=")) {
                loadedAnimationSpeed = Long.parseLong(line.split("=")[1].trim());
            }
        }

        // --- Загружаем слайды
        List<DefaultSlide> slides = new ArrayList<>();

        for (int i = 1; i <= slideCount; i++) {

            String imgName = String.format("slide_%04d.png", i);

            File imgFile = new File(tempDir.toFile(), "slides/" + imgName);
            File noteFile = new File(tempDir.toFile(), "notes/" + imgName.replace(".png", ".txt"));

            List<String> notes = noteFile.exists()
                    ? Files.readAllLines(noteFile.toPath())
                    : List.of();

            DefaultSlide slide = factory.createSlideWithNotes(imgFile, notes);

            slides.add(slide);
        }

        return new LoadedProject(slides, loadedAnimationType, loadedAnimationSpeed);
    }

    // архивирование
    private static void zipFolder(File src, File zipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile)))
        {
            Path base = src.toPath();
            Files.walk(base)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(path ->
                    {
                        try
                        {
                            String entryName = base.relativize(path).toString();
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (Exception ignored) {}
                    });
        }
    }

    // разархивирование
    private static void unzip(File zipFile, File destDir) throws Exception {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile));
        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File newFile = new File(destDir, entry.getName());
            newFile.getParentFile().mkdirs();
            java.nio.file.Files.copy(zis, newFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            zis.closeEntry();
        }
        zis.close();
    }

    // удаление папки
    private static void deleteDirectory(File dir) throws Exception
    {
        if (!dir.exists()) return;
        Files.walk(dir.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach(p -> p.toFile().delete());
    }
}