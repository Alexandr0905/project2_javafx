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
    public static void saveProject(File outZipFile, List<DefaultSlide> slides) throws Exception
    {
        Path tempDir = Files.createTempDirectory("myshow_project_");

        // meta
        File meta = new File(tempDir.toFile(), "meta.txt");
        try (PrintWriter pw = new PrintWriter(meta)) {
            pw.println("slideCount=" + slides.size());
        }

        // слайды
        File slidesDir = new File(tempDir.toFile(), "slides");
        slidesDir.mkdirs();

        // заметки
        File notesDir = new File(tempDir.toFile(), "notes");
        notesDir.mkdirs();

        for (int i = 0; i < slides.size(); i++) {
            DefaultSlide slide = slides.get(i);

            String imgName = String.format("slide_%04d.png", i+1);

            // копирование картинки
            Files.copy(
                    slide.getImageFile().toPath(),
                    new File(slidesDir, imgName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            // сохранение заметок
            File noteFile = new File(notesDir, imgName.replace(".png", ".txt"));
            try (PrintWriter pw = new PrintWriter(noteFile)) {
                for (String note : slide.getNotes()) pw.println(note);
            }
        }

        // zip
        zipFolder(tempDir.toFile(), outZipFile);

        // очищаем временную папку
        deleteDirectory(tempDir.toFile());
    }

    // загрузка проекта
    public static List<DefaultSlide> loadProject(File zipFile, SlideFactory factory) throws Exception
    {

        Path tempDir = Files.createTempDirectory("myshow_load_");

        unzip(zipFile, tempDir.toFile());

        File meta = new File(tempDir.toFile(), "meta.txt");
        List<String> lines = Files.readAllLines(meta.toPath());
        int slideCount = Integer.parseInt(lines.get(0).split("=")[1].trim());

        List<DefaultSlide> slides = new ArrayList<>();

        for (int i = 1; i <= slideCount; i++)
        {

            String imgName = String.format("slide_%04d.png", i);

            File imgFile = new File(tempDir.toFile(), "slides/" + imgName);
            File noteFile = new File(tempDir.toFile(), "notes/" + imgName.replace(".png", ".txt"));

            DefaultSlide slide = factory.createImageSlide(imgFile);

            if (noteFile.exists())
            {
                List<String> notes = Files.readAllLines(noteFile.toPath());
                for (String n : notes) slide.addNote(n);
            }

            slides.add(slide);
        }

        return slides;
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
    private static void unzip(File zipFile, File destDir) throws Exception
    {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile)))
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                File newFile = new File(destDir, entry.getName());
                newFile.getParentFile().mkdirs();
                Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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