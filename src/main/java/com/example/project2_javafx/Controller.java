package com.example.project2_javafx;

//import com.example.project2_javafx.Aggregate;
//import com.example.project2_javafx.ConcreteAggregate;
//import com.example.project2_javafx.Iterator;
import com.example.project2_javafx.model.DefaultSlide;
import com.example.project2_javafx.model.Slide;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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


    @FXML
    private void initialize() {
        updateDelayLabels(delaySlider.getValue());
        delaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateDelayLabels(newVal.doubleValue());
            setSlideshowDelayMillis(newVal.longValue());
        });
        // Настроим ListView для слайдов: отображаем имя файла (toString() в DefaultSlide уже возвращает имя файла)
        slidesListView.setItems(FXCollections.observableArrayList());
        slidesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showSlide(newSel);
                int idx = slidesListView.getSelectionModel().getSelectedIndex();
                currentSlideNumber = idx + 1;
                updateSlideState(); // ваш builder-метод (предполагается реализован)
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

            // Drag detected
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;
                javafx.scene.input.Dragboard db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(cell.getItem().getId());
                db.setContent(content);
                event.consume();
            });

            // Drag over
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                }
                event.consume();
            });

            // Drop
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
    }


//    // загрузка фото
//    @FXML
//    private void onLoadImage() {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.getExtensionFilters().add(
//                new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png", "*.webp")
//        );
//
//        currentFiles = fileChooser.showOpenMultipleDialog(getStage());
//        if (currentFiles != null && !currentFiles.isEmpty()) {
//            // создаём коллекцию и итератор
//            conaggr = new ConcreteAggregate(currentFiles);
//            iter = conaggr.getIterator();
//            // сразу показать первый кадр
//            Image img = iter.next();
//            if (img != null && !img.isError()) {
//                imageCollection.setImage(img);
//                imageCollection.setPreserveRatio(true);
//            }
//            // обновляем состояние
//            currentSlideNumber = 1;
//            totalSlides = currentFiles.size();
//            updateSlideState();
//        }
//    }

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
                currentSlides.add(new DefaultSlide(f));
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

    // показать слайд в ImageView и обновить notesListView
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        // обновляем область заметок
        notesListView.getItems().setAll(slide.getNotes());
    }

    // Next/Prev должны теперь использовать iter.next()/previous() и синхронизировать selection
    @FXML
    private void onRight() {
        if (iter == null) return;
        DefaultSlide s = iter.next();
        if (s != null) {
            showSlide(s);
            // Обновляем номер и selection в ListView
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


//    // действие нажатия на кнопку влево
//    @FXML
//    private void onLeft() {
//        if (iter == null) return;
//        Image img = iter.preview(); // движемся назад
//        if (img != null && !img.isError()) {
//            imageCollection.setImage(img);
//        }
//
//        currentSlideNumber--;
//        if (currentSlideNumber < 1) currentSlideNumber = totalSlides;
//        updateSlideState();
//    }
//
//    // действие нажатия на кнопку вправо
//    @FXML
//    private void onRight() {
//        if (iter == null) return;
//        Image img = iter.next(); // движемся вперёд
//        if (img != null && !img.isError()) {
//            imageCollection.setImage(img);
//        }
//
//        currentSlideNumber++;
//        if (currentSlideNumber > totalSlides) currentSlideNumber = 1;
//        updateSlideState();
//    }


    // ф-ия старта слайдшоу
    public void startSlideshow() {
        stopSlideshow();

        slideshow = new Timeline(new KeyFrame(Duration.millis(slideshowDelayMillis), e -> { onRight(); }));

        slideshow.setCycleCount(Timeline.INDEFINITE);
        slideshow.play();

        playPauseBtn.setText("Pause");
    }

    // ф-ия остановки слайдшоу
    public void stopSlideshow() {
        if (slideshow != null) {
            slideshow.stop();
            slideshow = null;
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
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить проект");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MyShow Project", "*.myshow"));
        File out = fc.showSaveDialog(getStage());
        if (out != null) {
            try {
                ProjectZipIO.saveProject(out, currentSlides);
            } catch (Exception e) { e.printStackTrace(); }
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
                currentSlides = ProjectZipIO.loadProject(zip);
                slidesListView.setItems(FXCollections.observableArrayList(currentSlides));

                conaggr = new ConcreteAggregate(currentSlides);
                iter = conaggr.getIterator();

                totalSlides = currentSlides.size();
                currentSlideNumber = 1;

                if (!currentSlides.isEmpty()) {
                    DefaultSlide first = iter.next();
                    showSlide(first);
                    slidesListView.getSelectionModel().select(0);
                }

                updateSlideState();

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onClearAll() {
        // остановить слайдшоу если идёт
        stopSlideshow();

        // очистить коллекции
        if (currentSlides != null) currentSlides.clear();

        slidesListView.getItems().clear();
        notesListView.getItems().clear();

        // сбросить изображение
        imageCollection.setImage(null);

        // сбросить состояние
        currentSlideNumber = 0;
        totalSlides = 0;
        updateSlideState();

        // сбросить итератор и агрегат
        iter = null;
        conaggr = null;

        // очистить поле ввода заметок
        noteInput.clear();
    }

}