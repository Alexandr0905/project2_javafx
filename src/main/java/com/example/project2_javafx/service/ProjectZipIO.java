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
                                   long animationSpeedMillis,
                                   File musicFile) throws Exception {

        Path tempDir = Files.createTempDirectory("myshow_project_");

        // meta
        File meta = new File(tempDir.toFile(), "meta.txt");
        try (PrintWriter pw = new PrintWriter(meta)) {
            pw.println("slideCount=" + slides.size());
            pw.println("animationType=" + (animationType != null ? animationType.name() : "FADE"));
            pw.println("animationSpeed=" + animationSpeedMillis);

            // music
            pw.println("musicFile=" + (musicFile != null ? musicFile.getName() : ""));
        }

        // slides and notes
        File slidesDir = new File(tempDir.toFile(), "slides");
        slidesDir.mkdirs();
        File notesDir = new File(tempDir.toFile(), "notes");
        notesDir.mkdirs();

        for (int i = 0; i < slides.size(); i++) {
            DefaultSlide slide = slides.get(i);
            String imgName = String.format("slide_%04d.png", i + 1);

            // image
            Files.copy(slide.getImageFile().toPath(),
                    new File(slidesDir, imgName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            // notes
            File noteFile = new File(notesDir, imgName.replace(".png", ".txt"));
            try (PrintWriter npw = new PrintWriter(noteFile)) {
                for (String note : slide.getNotes()) npw.println(note);
            }
        }

        // music dir
        if (musicFile != null && musicFile.exists()) {
            File musicDir = new File(tempDir.toFile(), "music");
            musicDir.mkdirs();

            Files.copy(musicFile.toPath(),
                    new File(musicDir, musicFile.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // zip
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
                        } catch (Exception ignored) {}
                    });
        }

        // clean
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> p.toFile().delete());
    }

    public record LoadedProject(List<DefaultSlide> slides,
                                AnimationType animationType,
                                long animationSpeed,
                                File musicFile) {}

    // загрузка проекта
    public static LoadedProject loadProject(File zipFile, SlideFactory factory) throws Exception {

        Path tempDir = Files.createTempDirectory("myshow_load_");

        unzip(zipFile, tempDir.toFile());

        File meta = new File(tempDir.toFile(), "meta.txt");
        List<String> lines = Files.readAllLines(meta.toPath());

        int slideCount = 0;
        AnimationType loadedAnimationType = AnimationType.FADE;
        long loadedAnimationSpeed = 400;

        String musicName = "";

        // meta read
        for (String line : lines) {
            if (line.startsWith("slideCount="))
                slideCount = Integer.parseInt(line.split("=")[1].trim());

            if (line.startsWith("animationType="))
                loadedAnimationType = AnimationType.valueOf(line.split("=")[1].trim());

            if (line.startsWith("animationSpeed="))
                loadedAnimationSpeed = Long.parseLong(line.split("=")[1].trim());

            if (line.startsWith("musicFile="))
                musicName = line.split("=")[1].trim();
        }

        // load slides
        List<DefaultSlide> slides = new ArrayList<>();

        for (int i = 1; i <= slideCount; i++) {
            String imgName = String.format("slide_%04d.png", i);

            File imgFile = new File(tempDir.toFile(), "slides/" + imgName);
            File noteFile = new File(tempDir.toFile(), "notes/" + imgName.replace(".png", ".txt"));

            List<String> notes = noteFile.exists()
                    ? Files.readAllLines(noteFile.toPath())
                    : List.of();

            DefaultSlide sl = factory.createSlideWithNotes(imgFile, notes);
            slides.add(sl);
        }

        // load music
        File musicFile = null;
        if (!musicName.isEmpty()) {
            File musicDir = new File(tempDir.toFile(), "music/" + musicName);
            if (musicDir.exists()) {
                musicFile = musicDir;
            }
        }

        return new LoadedProject(slides, loadedAnimationType, loadedAnimationSpeed, musicFile);
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
    private static void unzip(File zipFile, File targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                File outFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    Files.copy(zis, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
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