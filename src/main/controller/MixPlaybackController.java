package main.controller;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

public class MixPlaybackController {

    private MainController mainController;
    private Media media;
    private MediaPlayer mediaPlayer;
    private Duration trackDuration;
    private double lastDurationSecs;    // used to track changes in duration
    private ImageView playImgView;
    private ImageView pauseImgView;

    @FXML private BorderPane borderPane;
    @FXML private Label currentDurLabel;
    @FXML private Label totalDurLabel;
    @FXML private Button ppButton;
    @FXML private Slider trackSlider;

    MixPlaybackController() {
        Image playButton = new Image(getClass().getClassLoader().
                getResourceAsStream("images/play_50x50.png"));
        Image pauseButton = new Image(getClass().getClassLoader().
                getResourceAsStream("images/pause_50x50.png"));
        playImgView = new ImageView(playButton);
        pauseImgView = new ImageView(pauseButton);
        lastDurationSecs = 0.0;
    }

    void init(MainController mainController, File file) {
        this.mainController = mainController;
        media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setOnReady(() -> {
            trackDuration = media.getDuration();
            totalDurLabel.setText(mainController.convertDuration(trackDuration));
        });
        ppButton.setGraphic(pauseImgView);

        // Listener for slider bar drags
        trackSlider.valueProperty().addListener((Observable ob) -> {
            if (trackSlider.isValueChanging()) {
                if (trackDuration != null) {
                    mediaPlayer.seek(trackDuration.multiply(trackSlider.getValue() / 100.0));
                }
            }
        });

        // Listener for changes in time
        mediaPlayer.currentTimeProperty().addListener(
            (ObservableValue<? extends Duration> ob, Duration oldVal, Duration newVal) -> {
                currentDurLabel.setText(mainController.convertDuration(newVal));
                trackSlider.valueProperty().setValue(newVal.divide(trackDuration.toMillis()).toMillis() * 100.0);
                // Update track name only if duration has changed more than 0.5 seconds
                if (Math.abs(newVal.toSeconds() - lastDurationSecs) > 0.5) {
                    lastDurationSecs = newVal.toSeconds();
                    mainController.updateTrackID(newVal);
                }
        });
    }


    public void ppButtonPressed (ActionEvent actionEvent) {
        MediaPlayer.Status status = mediaPlayer.getStatus();
        switch (status) {
            case READY:
            case PAUSED:
            case STOPPED:
                mediaPlayer.play();
                ppButton.setGraphic(pauseImgView);
                break;
            default:
                mediaPlayer.pause();
                ppButton.setGraphic(playImgView);
                break;
        }
    }

    void shutdown() {
        mediaPlayer.stop();
    }

}
