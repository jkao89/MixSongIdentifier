package main.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.models.AuthorizationCodeCredentials;
import com.wrapper.spotify.models.User;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MainController {

    private File file;  // Continous mix file
    private Api api;    // Michael Thelin's Spotify web api Java wrapper
    private MixPlaybackController mixPBController;      // Controller for mix playback
    private TrackDisplayController trackDController;    // Controller for track identification and display
    private boolean switchMixFlag;      // Set after user changes mix file

    @FXML BorderPane outerBorderPane;
    @FXML BorderPane leftBorderPane;
    @FXML HBox buttonHBox;
    @FXML Button openFileButton;
    @FXML Button spotifyLoginButton;
    @FXML ListView<String> trackListView;   // Displays the track list for this mix
    @FXML private Text mixName;     // File name of mix

    public void initialize() {
        try {
            Image spotifyLogoImg = new Image(getClass().getClassLoader().
                    getResourceAsStream("images/spotify_logo_26x26.png"));
            ImageView spotifyLogoBtnView = new ImageView(spotifyLogoImg);
            spotifyLoginButton.setGraphic(spotifyLogoBtnView);
        } catch (Exception e) {
            System.out.println("Error loading Spotify button");
        }
        switchMixFlag = false;
    }

    // Triggered when user clicks on "Open" button
    public void selectMixAndPlay(ActionEvent actionEvent) throws Exception {
        selectMix();
        if (file != null) {
            initControllers();
        }
    }

    // Opens file chooser for user to select mix file
    private void selectMix() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Audio files", "*.mp3", "*.wav", "*.m4a", "*.flac", "*.aac");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Select DJ mix file");
        file = fileChooser.showOpenDialog(openFileButton.getScene().getWindow());
    }

    // Initializes and sets the mix playback and track display controllers
    private void initControllers() throws Exception {
        // If a track is currently playing, clear everything
        if (mixPBController != null && trackDController != null) {
            switchMixFlag = true;
            trackListView.getItems().clear();
            mixPBController.shutdown();
            trackDController.shutdown();
            leftBorderPane.setCenter(null);
            leftBorderPane.setBottom(null);
        }
        // Set the file name
        mixName.setText(file.getName());
        // Initialize new controllers
        mixPBController = new MixPlaybackController();
        trackDController = new TrackDisplayController(20);

        // Load FXML file and set controllers
        FXMLLoader PBloader = new FXMLLoader(getClass().getResource("/main/view/mixPlayBack.fxml"));
        PBloader.setController(mixPBController);
        leftBorderPane.setCenter(PBloader.load());
        FXMLLoader Dloader = new FXMLLoader(getClass().getResource("/main/view/trackDisplay.fxml"));
        Dloader.setController(trackDController);
        leftBorderPane.setBottom(Dloader.load());

        mixPBController.init(this, file);
        trackDController.init(this, file);
        if (api != null) {
            setTrackDisplayAPI(api);
        }
    }

    private void setTrackDisplayAPI(Api api) {
        trackDController.setAPI(api);
    }

    // Updates track list for current mix
    // switchMixFlag prevents listview from being updated after switching to a new mix
    void updateTrackList(ArrayList<Map.Entry<String, String>> trackList) {
        if (switchMixFlag) {
            switchMixFlag = false;
        } else {
            ObservableList<String> tracks = FXCollections.observableArrayList();
            for (Map.Entry<String, String> trackEntry : trackList) {
                tracks.add(trackEntry.getKey() + "   " + trackEntry.getValue());
            }
            trackListView.setItems(tracks);
        }
    }

    // Function is called by associated mix playback controller from a change listener on the current
    // time property of the media player
    void updateTrackID(Duration duration) {
        trackDController.getCurrTrack(duration);
    }

    // Function to convert from Duration to hh:mm:ss format
    String convertDuration(Duration duration) {
        int secs = (int) Math.floor(duration.toSeconds());
        int hours = secs / 3600;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, (secs % 3600) / 60,
                    (secs % 60));
        } else {
            return String.format("%02d:%02d", secs / 60, (secs % 60));
        }
    }

    // Triggered when user clicks on Spotify icon
    // Creates a simple server to handle Spotify's redirected callback URI
    // Initializes and sets access and refresh tokens for api object from Michael Thelin's
    // Spotify web java API.
    public void spotifyLogin(ActionEvent actionEvent) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);   // Used to wait on response from server
        HttpServer httpserver = HttpServer.create(new InetSocketAddress(8888), 0);
        httpserver.createContext("/", new MainController.SpotifyHandler(latch));
        httpserver.start();
        final String clientID = "0280cdd3b5254b1cb356500a526579b8";
        final String clientSecret = "d2f76983e64d4760b095285862107efb";
        final String redirectURI = "http://localhost:8888/";
        api = Api.builder()
                .clientId(clientID)
                .clientSecret(clientSecret)
                .redirectURI(redirectURI)
                .build();
        final List<String> scopes = Arrays.asList("user-read-private",
                "user-library-read", "user-library-modify");
        final String state = "cg5VaBYwRZF4o2Nw";
        String authorizeURL = api.createAuthorizeURL(scopes, state);
        Stage currStage = (Stage) openFileButton.getScene().getWindow();
        HostServices hostServices = (HostServices) currStage.getProperties().get("hostServices");
        hostServices.showDocument(authorizeURL); // Open Spotify authentication url in browser
        latch.await();      // Wait until server has responded and called countdown() on latch
        // Split query string and get code
        String[] params = MainController.SpotifyHandler.getQueryString().split("&");
        String code = params[0].substring(params[0].indexOf("=")+1);

        httpserver.stop(0);
        AuthorizationCodeCredentials authCodeCred = api.authorizationCodeGrant(code).build().get();
        api.setAccessToken(authCodeCred.getAccessToken());
        api.setRefreshToken(authCodeCred.getRefreshToken());

        // Send request to get user profile information
        CurrentUserRequest request = api.getMe().build();
        try {
            User user = request.get();
            List<com.wrapper.spotify.models.Image> userImages = user.getImages();
            com.wrapper.spotify.models.Image profileImgModel = userImages.get(0);
            String url = profileImgModel.getUrl();
            Image profileImg = new Image(url, 40, 40, true, true);
            buttonHBox.getChildren().remove(spotifyLoginButton);
            ImageView profileImgView = new ImageView(profileImg);
            // Add user profile image and user ID to scene
            buttonHBox.getChildren().add(profileImgView);
            buttonHBox.getChildren().add(new Label(user.getId()));
        } catch (Exception e) {
            System.out.println("Error getting user profile information");
            System.out.println(e.getMessage());
        }
        System.out.println("Access token successfully retrieved!");
        // If user logs in after opening mix file
        if (trackDController != null) {
            if (!trackDController.apiSet()) {
                setTrackDisplayAPI(api);
            }
        }
    }

    static class SpotifyHandler implements HttpHandler {

        static private String queryString;
        CountDownLatch latch;

        SpotifyHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void handle(HttpExchange h) throws IOException {
            String response = "Success";
            h.sendResponseHeaders(200, response.length());
            URI uri = h.getRequestURI();
            queryString = uri.getQuery();
            OutputStream out = h.getResponseBody();
            out.write(response.getBytes());
            out.close();
            latch.countDown();
        }

        static String getQueryString() {
            return queryString;
        }

    }
}

