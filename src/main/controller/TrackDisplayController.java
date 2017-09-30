package main.controller;

import com.acrcloud.utils.ACRCloudRecognizer;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.AddToMySavedTracksRequest;
import com.wrapper.spotify.methods.ContainsMySavedTracksRequest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import main.model.MixObj;
import main.model.MixResults;
import main.model.Track;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class TrackDisplayController {

    private MainController mainController;
    private final int idInterval;   // Interval at which requests to ACRCloud are sent, in seconds
    private Api api;                // Michael Thelin's Spotify web api Java wrapper
    private ArrayList<Map.Entry<String, String>> trackList;   // Stores track list for this mix file
    private ACRCloudRecognizer recognizer;
    private ExecutorService exec;
    private MixObj mixobj;
    private MixResults mixresults;
    private int idCount;       // Number of ACRCloud requests sent so far
    private CountDownLatch displaySignal;   // Used to wait for first ACRCloud request to finish
    private String lastTrackACRID;          // ACRCloud id of last request
    private Track currTrack;                // Current track being played
    private String titleStr;                // Track title of currTrack
    private String artistsStr;              // Artist names of currTrack
    private ImageView plusImgView;
    private ImageView checkImgView;
    private Future<?> idTracksFuture;       // Future of task identifying tracks

    TrackDisplayController(int idInterval) {
        this.idInterval = idInterval;
        mixresults = new MixResults();
        idCount = 0;
        displaySignal = new CountDownLatch(1);
        trackList = new ArrayList<>();
        lastTrackACRID = "";
        Image plusImg = new Image(getClass().getClassLoader().
                getResourceAsStream("images/plus_20x21.png"));
        Image checkImg = new Image(getClass().getClassLoader().
                getResourceAsStream("images/check_27x20.png"));
        plusImgView = new ImageView(plusImg);
        checkImgView = new ImageView(checkImg);
    }

    @FXML private Label trackTitle;
    @FXML private Label artistNames;
    @FXML private Button addTrackButton;

    void init(MainController mainController, File file) {
        this.mainController = mainController;
        mixobj = new MixObj(file);
        if (apiSet()) {
            addTrackButton.setVisible(true);
            addTrackButton.setDisable(false);
            addTrackButton.setGraphic(plusImgView);
        }
        ACRCloudRecognizerInit();
        exec = Executors.newFixedThreadPool(2,
                (Runnable r) -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                });
        idTracksInMix();
    }

    // Identifies tracks in mix based on idInterval
    private void idTracksInMix() {
        Task<Void> trackIDTask = new Task<Void>() {
            @Override
            protected Void call() {
                String result;  // JSON result returned from ACRCloud
                int offsetSecs = 0;   // Seconds from beginning of mix
                while (offsetSecs < (mixobj.getFileDurationMS() / 1000)) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    result = recognizer.recognizeByFileBuffer( // Send and get result from ACRCloud
                            mixobj.getFileBuffer(), mixobj.getFileBufferLen(), offsetSecs);
                    Track track = mixresults.addTrack(result);
                    idCount++;
                    if (!(track.getStatusMsg()).equals("No result")) {      // If track is identified
                        if (!lastTrackACRID.equals(track.getACRid())) {     // If track has changed
                            lastTrackACRID = track.getACRid();              // Update variable
                            Duration lastDur = Duration.seconds(offsetSecs);
                            String trackInfo = track.getTrackArtists() + " - " + track.getTrackTitle();
                            trackList.add(new AbstractMap.SimpleEntry(
                                    mainController.convertDuration(lastDur), trackInfo));
                        }
                    } else {                                                // If track not identified
                        if (!lastTrackACRID.equals("") || offsetSecs == 0) {
                            lastTrackACRID = "";
                            Duration lastDur = Duration.seconds(offsetSecs);
                            trackList.add(new AbstractMap.SimpleEntry(
                                    mainController.convertDuration(lastDur), "Unknown track"));
                        }
                    }
                    Platform.runLater(() -> {
                        mainController.updateTrackList(trackList);
                    });
                    //System.out.println(offsetSecs);
                    //System.out.println(result);
                    offsetSecs += idInterval;
                    if (displaySignal.getCount() > 0) {
                        displaySignal.countDown();
                    }
                }
                return null;
            }
        };
        idTracksFuture = exec.submit(trackIDTask);
    }

    void setAPI(Api api) {
        this.api = api;
        if (addTrackButton != null) {
            addTrackButton.setVisible(true);
            addTrackButton.setDisable(false);
            addTrackButton.setGraphic(plusImgView);
        }
    }

    boolean apiSet() {
        return (api != null);
    }

    // Gets and displays track from mixresults based on duration
    void getCurrTrack(Duration duration) {
        Task<Void> getCurrTrackTask = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    displaySignal.await();            // Wait for first ACRCloud request to finish
                } catch (InterruptedException ex) {
                    System.out.println("getCurrTrack thread interrupted while waiting");
                }
                boolean trackUnknown;
                int secs = (int) Math.floor(duration.toSeconds());  // Current duration in seconds
                if ((double)(secs / idInterval) < idCount) {   // Check duration vs progress
                    currTrack = mixresults.getTrack(secs, idInterval);
                    if (!(currTrack.getStatusMsg()).equals("No result")) {  // If track was identified
                        trackUnknown = false;
                        if (api != null) {      // If signed into Spotify
                            spotifyBtnProcessor(currTrack.getSpotifyTrackID());
                        }
                        titleStr = currTrack.getTrackTitle();
                        artistsStr = currTrack.getTrackArtists();
                    } else {    // Unknown track
                        trackUnknown = true;
                    }
                } else {    // Duration beyond progress of idTrack thread
                    trackUnknown = true;
                }
                if (trackUnknown) {
                    if (api != null) {
                        spotifyBtnProcessor("");
                    }
                    titleStr = "Unknown track";
                    artistsStr = "";
                }
                Platform.runLater(() -> {
                    trackTitle.setText(titleStr);
                    artistNames.setText(artistsStr);
                });
                return null;
            }
        };
        exec.execute(getCurrTrackTask);
    }

    // Updates button used for adding songs to Spotify account
    // Check displayed when track already in user's library
    // Button disabled if track unknown
    private void spotifyBtnProcessor (String spotifyTrackID) {
        Runnable setCheck = () -> {
            if (addTrackButton.getGraphic() == plusImgView) {
                addTrackButton.setGraphic(checkImgView);
                addTrackButton.setDisable(true);
            }
        };

        Runnable setPlus = () -> {
            if (addTrackButton.getGraphic() == checkImgView) {
                addTrackButton.setGraphic(plusImgView);
            }
        };

        if (!spotifyTrackID.isEmpty()) {    // If track was identified
            ContainsMySavedTracksRequest request = api.containsMySavedTracks(
                    Collections.singletonList(spotifyTrackID)).build(); // Request to check if track already saved
            try {
                List<Boolean> containTracks = request.get();
                if (containTracks.get(0)) {                    // Track already in user's library
                    Platform.runLater(setCheck);
                } else {                                       // Track not in user's library
                    Platform.runLater(setPlus);
                    if (addTrackButton.isDisabled()) {
                        addTrackButton.setDisable(false);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error processing Spotify button");
            }
        } else {    // Track was not identified
            Platform.runLater(setPlus);
            addTrackButton.setDisable(true);
        }
    }

    // Handler for button to add tracks to Spotify
    public void addTrackToSpotify(ActionEvent actionEvent) {
        List<String> tracksToAdd = Collections.singletonList(currTrack.getSpotifyTrackID());
        AddToMySavedTracksRequest request = api.addToMySavedTracks(tracksToAdd).build();
        try {
            request.get();
            System.out.println("Added track to your music");
        } catch (Exception e) {
            System.out.println("Error adding track");
        }
    }

    // Initializes ACRCloudRecognizer object
    private void ACRCloudRecognizerInit() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", "identify-us-west-2.acrcloud.com");
        config.put("access_key", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        config.put("access_secret", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        config.put("debug", false);
        config.put("timeout", 30); // seconds
        recognizer = new ACRCloudRecognizer(config);
    }

    // Called when user switches mix file
    void shutdown() {
        idTracksFuture.cancel(true);
        exec.shutdownNow();
    }

}





