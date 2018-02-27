# MixSongIdentifier
JavaFX desktop application for identifying tracks in DJ style mixes

This application uses ACRCloud for music recognition, Michael Thelin's Spotify Web API Java wrapper for Spotify interactions, and GSON for
conversion from JSON to Java objects. 

The app acts as a media player for DJ style mixes. While the mix is playing, intervals of the mix are sent to ACRCloud in a linear
fashion. For example, when a mix begins playing, the first 0 to x seconds are sent for identification, then x to 2x seconds, and so on.

As tracks are identified, the corresponding track list in the right pane will be updated. If identified, the title and artists of the track
which is currently playing will be displayed to the user. The user has the option of logging into their Spotify account, so that they
can add tracks from the mix to their library.

