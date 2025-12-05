package com.example.project2_javafx;

//import com.example.project2_javafx.Aggregate;
//import com.example.project2_javafx.ConcreteAggregate;
//import com.example.project2_javafx.Iterator;
import com.example.project2_javafx.factory.DefaultSlideFactory;
import com.example.project2_javafx.factory.SlideFactory;
import com.example.project2_javafx.model.DefaultSlide;
import com.example.project2_javafx.model.Slide;
import com.example.project2_javafx.service.AnimationType;
import com.example.project2_javafx.service.ProjectZipIO;
import com.example.project2_javafx.slides.ConcreteAggregate;
import com.example.project2_javafx.slides.SlideIterator;
import com.example.project2_javafx.slides.SlideAggregate;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.print.attribute.standard.Media;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML private ImageView imageCollection;
    @FXML private Button loadCollectionBtn;
    @FXML private Pane pane;
    @FXML private Slider delaySlider;
    @FXML private Label delayLabel;
    @FXML private Button playPauseBtn;
    @FXML private ListView<DefaultSlide> slidesListView;
    @FXML private ListView<String> notesListView;
    @FXML private TextArea noteInput;
    @FXML private Button addNoteBtn;
    @FXML private Button clearNotesBtn;
    @FXML private Button saveSlideBtn;
    @FXML private ComboBox<String> animationComboBox;
    @FXML private Slider animationSpeedSlider;
    @FXML private Label animationSpeedLabel;
    @FXML private Button loadMusicBtn;
    @FXML private Label musicLabel;

    // Music
    private File currentMusicFile = null;
    private MediaPlayer mediaPlayer = null;

    private AnimationType animationType = AnimationType.FADE;
    private long animationDurationMillis = 400; // default


    // для надписи 0 из 0 слайдов
    @FXML
    private Label stateLabel;
    private int currentSlideNumber = 1;
    private int totalSlides = 0;

    // слайды и итератор
    private List<DefaultSlide> currentSlides;
    private ConcreteAggregate conaggr;
    private SlideIterator iter;

    // для автоплея
    private Timeline slideshow;
    private long slideshowDelayMillis = 2000; // дефолт 2 секунды

    private Stage getStage() {
        return (Stage) pane.getScene().getWindow();
    }

    private SlideFactory slideFactory = new DefaultSlideFactory();



    @FXML
    private void initialize() {
        updateDelayLabels(delaySlider.getValue());
        delaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateDelayLabels(newVal.doubleValue());
            setSlideshowDelayMillis(newVal.longValue());
        });
        slidesListView.setItems(FXCollections.observableArrayList());
        slidesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showSlide(newSel);
                int idx = slidesListView.getSelectionModel().getSelectedIndex();
                currentSlideNumber = idx + 1;
                updateSlideState();
            }
        });

        notesListView.setItems(FXCollections.observableArrayList());

        slidesListView.setCellFactory(lv -> {
            ListCell<DefaultSlide> cell = new ListCell<>() {
                @Override
                protected void updateItem(DefaultSlide item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString()); // можно заменить на миниатюру
                    }
                }
            };

            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;
                javafx.scene.input.Dragboard db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(cell.getItem().getId());
                db.setContent(content);
                event.consume();
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    String draggedId = db.getString();
                    DefaultSlide draggedSlide = findSlideById(draggedId);
                    int dropIndex = cell.getIndex();

                    if (draggedSlide != null) {
                        int oldIndex = currentSlides.indexOf(draggedSlide);
                        if (oldIndex != dropIndex) {
                            currentSlides.remove(oldIndex);
                            if (dropIndex > currentSlides.size()) dropIndex = currentSlides.size();
                            currentSlides.add(dropIndex, draggedSlide);

                            // обновляем ListView
                            slidesListView.getItems().setAll(currentSlides);

                            // синхронизируем итератор
                            conaggr = new ConcreteAggregate(currentSlides);
                            iter = conaggr.getIterator();

                            // обновляем currentSlideNumber
                            currentSlideNumber = slidesListView.getSelectionModel().getSelectedIndex() + 1;
                            updateSlideState();
                        }
                    }
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });

        // выбор анимации
        animationComboBox.getItems().addAll("Fade", "Slide", "Zoom");
        animationComboBox.setValue("Fade"); // дефолт
        animationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "Fade" -> animationType = AnimationType.FADE;
                case "Slide" -> animationType = AnimationType.SLIDE;
                case "Zoom" -> animationType = AnimationType.ZOOM;
            }
        });

        // скорость анимации
        animationSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            animationDurationMillis = newVal.longValue();
            if (animationSpeedLabel != null) {
                animationSpeedLabel.setText(String.format("%d мс", animationDurationMillis));
            }
        });
    }

    // обработчик загрузки
    @FXML
    private void onLoadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );

        List<File> chosen = fileChooser.showOpenMultipleDialog(getStage());
        if (chosen != null && !chosen.isEmpty()) {
            // создаём slides
            currentSlides = new ArrayList<>();
            for (File f : chosen) {
                currentSlides.add(slideFactory.createImageSlide(f));
            }

            // создаём агрегат и итератор
            conaggr = new ConcreteAggregate(currentSlides);
            iter = conaggr.getIterator();

            // заполняем ListView
            ObservableList<DefaultSlide> obs = FXCollections.observableArrayList(currentSlides);
            slidesListView.setItems(obs);

            // показаем первый слайд
            DefaultSlide first = iter.next();
            if (first != null) {
                showSlide(first);
                currentSlideNumber = 1;
                totalSlides = conaggr.size();
                slidesListView.getSelectionModel().select(0); // выделить первый
                updateSlideState();
            }
        }
    }

    private DefaultSlide findSlideById(String id) {
        for (DefaultSlide s : currentSlides) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    // обновление статуса и кол-ва слайдов 0 из 0
    private void updateSlideState() {
        SlideStateBuilder state = new SlideStateBuilder.Builder()
                .current(currentSlideNumber)
                .total(totalSlides)
                .build();

        stateLabel.setText(state.getText());
    }

    private void showSlide(DefaultSlide slide) {
        if (slide == null) {
            imageCollection.setImage(null);
            notesListView.getItems().clear();
            return;
        }
        try {
            Image img = new Image(slide.getImageFile().toURI().toString());
            imageCollection.setImage(img);
            imageCollection.setPreserveRatio(true);

            applyAnimation(imageCollection, animationType);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // обновляем область заметок
        notesListView.getItems().setAll(slide.getNotes());
    }

    @FXML
    private void onRight() {
        if (iter == null) return;
        DefaultSlide s = iter.next();
        if (s != null) {
            showSlide(s);
            int idx = findSlideIndex(s);
            currentSlideNumber = idx + 1;
            slidesListView.getSelectionModel().select(idx);
            updateSlideState();
        }
    }

    @FXML
    private void onLeft() {
        if (iter == null) return;
        DefaultSlide s = iter.previous();
        if (s != null) {
            showSlide(s);
            int idx = findSlideIndex(s);
            currentSlideNumber = idx + 1;
            slidesListView.getSelectionModel().select(idx);
            updateSlideState();
        }
    }

    private int findSlideIndex(DefaultSlide s) {
        if (currentSlides == null || s == null) return -1;
        for (int i = 0; i < currentSlides.size(); i++) {
            if (currentSlides.get(i).getId().equals(s.getId())) return i;
        }
        return -1;
    }

    // Добавление заметки к текущему слайду
    @FXML
    private void onAddNote() {
        DefaultSlide sel = slidesListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String text = noteInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        sel.addNote(text.trim());
        // обновляем UI
        notesListView.getItems().add(text.trim());
        noteInput.clear();
    }

    // Очистить все заметки для текущего слайда
    @FXML
    private void onClearNotes() {
        DefaultSlide sel = slidesListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        sel.clearNotes();
        notesListView.getItems().clear();
    }


    // ф-ия старта слайдшоу
    public void startSlideshow() {
        stopSlideshow();

        slideshow = new Timeline(new KeyFrame(Duration.millis(slideshowDelayMillis), e -> { onRight(); }));

        slideshow.setCycleCount(Timeline.INDEFINITE);
        slideshow.play();

        // музыка
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.play();
        }

        playPauseBtn.setText("Pause");
    }

    // ф-ия остановки слайдшоу
    public void stopSlideshow() {
        if (slideshow != null) {
            slideshow.stop();
            slideshow = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }

        if (playPauseBtn != null)
            playPauseBtn.setText("Play");
    }

    // динамически задать время задержки и перезапустить слайдшоу, если оно было запущено
    public void setSlideshowDelayMillis(long millis) {
        slideshowDelayMillis = Math.max(100, millis);
        boolean wasRunning = slideshow != null;
        if (wasRunning) {
            startSlideshow();
        }
    }

    // обновление значений задержки слайдшоу
    private void updateDelayLabels(double millis) {
        double seconds = millis / 1000.0;

        if (delayLabel != null) {
            delayLabel.setText(String.format("Задержка: %.1f сек", seconds));
        }
    }

    // старт/стоп слайдшоу
    @FXML
    private void onPlayPause() {
        if (slideshow == null) {
            // Запуск
            startSlideshow();
            playPauseBtn.setText("Pause");
        } else {
            // Остановка
            stopSlideshow();
            playPauseBtn.setText("Play");
        }
    }

    @FXML
    private void onSaveSlide() {
        DefaultSlide sel = slidesListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить слайд как PNG");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setInitialFileName("slide_" + currentSlideNumber + ".png");

        File outFile = fileChooser.showSaveDialog(getStage());
        if (outFile != null) {
            // рендерим и сохраняем
            com.example.project2_javafx.service.SlideRenderer.renderToFile(sel, outFile);
        }
    }

    @FXML
    private void onSaveProject() {
        if (currentSlides == null || currentSlides.isEmpty()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить проект");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyShow Project", "*.myshow"));
        File out = fc.showSaveDialog(getStage());
        if (out != null) {
            try {
                // передаём текущие параметры анимации
                ProjectZipIO.saveProject(out, currentSlides, animationType, animationDurationMillis, currentMusicFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onLoadProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Загрузить проект");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyShow Project", "*.myshow"));
        File zip = fc.showOpenDialog(getStage());
        if (zip != null) {
            try {
                ProjectZipIO.LoadedProject lp = ProjectZipIO.loadProject(zip, slideFactory);

                currentSlides = lp.slides();
                slidesListView.setItems(FXCollections.observableArrayList(currentSlides));

                conaggr = new ConcreteAggregate(currentSlides);
                iter = conaggr.getIterator();

                totalSlides = currentSlides.size();
                currentSlideNumber = 1;

                animationType = lp.animationType();
                animationDurationMillis = lp.animationSpeed();

                currentMusicFile = lp.musicFile();

                animationComboBox.setValue(animationType.name().substring(0,1) + animationType.name().substring(1).toLowerCase());
                animationSpeedSlider.setValue(animationDurationMillis);
                animationSpeedLabel.setText(animationDurationMillis + " мс");

                if (!currentSlides.isEmpty()) {
                    DefaultSlide first = iter.next();
                    showSlide(first);
                    slidesListView.getSelectionModel().select(0);
                    currentSlideNumber = 1;
                }

                if (currentMusicFile != null) {
                    playMusic(currentMusicFile);
                }

                updateSlideState();

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onClearAll() {
        stopSlideshow();

        if (currentSlides != null) currentSlides.clear();

        slidesListView.getItems().clear();
        notesListView.getItems().clear();

        imageCollection.setImage(null);

        currentSlideNumber = 0;
        totalSlides = 0;
        updateSlideState();

        iter = null;
        conaggr = null;

        // очистить поле ввода заметок
        noteInput.clear();

        //сброс анимации
        animationDurationMillis = 400;
        if (animationSpeedLabel != null) {
            animationSpeedLabel.setText(String.format("%d мс", animationDurationMillis));
        }
    }

    @FXML
    private void onDeleteSlide() {
        DefaultSlide selected = slidesListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        currentSlides.remove(selected);

        slidesListView.getItems().setAll(currentSlides);

        conaggr = new ConcreteAggregate(currentSlides);
        iter = conaggr.getIterator();

        totalSlides = currentSlides.size();
        if (!currentSlides.isEmpty()) {
            DefaultSlide first = iter.next();
            showSlide(first);
            slidesListView.getSelectionModel().select(0);
            currentSlideNumber = 1;
        } else {
            imageCollection.setImage(null);
            notesListView.getItems().clear();
            currentSlideNumber = 0;
        }
        updateSlideState();
    }

    private void applyAnimation(ImageView imageView, AnimationType type) {
        switch (type) {
            case FADE -> playFade(imageView);
            case SLIDE -> playSlide(imageView);
            case ZOOM -> playZoom(imageView);
        }
    }

    private void playFade(ImageView iv) {
        iv.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(animationDurationMillis), iv);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void playSlide(ImageView iv) {
        iv.setTranslateX(100);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(animationDurationMillis), iv);
        tt.setFromX(100);
        tt.setToX(0);
        tt.play();
    }

    private void playZoom(ImageView iv) {
        iv.setScaleX(0.85);
        iv.setScaleY(0.85);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(animationDurationMillis), iv);
        st.setFromX(0.85);
        st.setFromY(0.85);
        st.setToX(1);
        st.setToY(1);
        st.play();
    }

    @FXML
    private void onChooseMusic() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Выбрать музыкальный файл");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.aac"));

        File file = fc.showOpenDialog(getStage());
        if (file != null) {
            currentMusicFile = file;

            musicLabel.setText("Музыка: " + file.getName());

            playMusic(file);
        }
    }

    private void playMusic(File f) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            javafx.scene.media.Media media = new javafx.scene.media.Media(f.toURI().toString());
            mediaPlayer = new javafx.scene.media.MediaPlayer(media);
            mediaPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
            mediaPlayer.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}