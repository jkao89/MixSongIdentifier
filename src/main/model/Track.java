package main.model;

import java.util.ArrayList;

public class Track {
    private Status status;
    private Metadata metadata;
    public int result_type;

    public String getStatusMsg() {
        return status.msg;
    }

    public String getTrackTitle() {
        return metadata.music.get(0).title;
    }

    public String getTrackArtists() {
        return String.join(", ", convertArtistNames());
    }

    public String getACRid() {
        return metadata.music.get(0).acrid;
    }

    public String getSpotifyTrackID() {
        return metadata.music.get(0).external_metadata.spotify.track.id;
    }

    private ArrayList<String> convertArtistNames() {
        ArrayList<String> artistNames = new ArrayList<>();
        for (int i = 0; i < metadata.music.get(0).artists.size(); i++) {
            artistNames.add(metadata.music.get(0).artists.get(i).toString());
        }
        return artistNames;
    }
}

class Status {
    String msg;
    int code;
    String version;
}

class Metadata {
    ArrayList<MusicObj> music;
    String timestamp_utc;
}

class MusicObj {
    ExternalID external_ids;
    String sample_begin_time_offset_ms;
    String label;
    ExternalMetadata external_metadata;
    String play_offset_ms;
    ArrayList<ArtistObj> artists;
    String sample_end_time_offset_ms;
    String release_date;
    String title;
    String db_end_time_offset_ms;
    int duration_ms;
    Album album;
    String acrid;
    int result_from;
    String db_begin_time_offset_ms;
    int score;
}

class ExternalID {
    String isrc;
    String upc;
}

class ExternalMetadata {
    Spotify spotify;
}

class Spotify {
    SpotAlbum album;
    ArrayList<SpotArtist> artists;
    SpotTrack track;
}

class SpotAlbum {
    String id;
}

class SpotArtist {
    String id;
}

class SpotTrack {
    String id;
}

class ArtistObj {
    private String name;

    @Override
    public String toString() {
        return name;
    }


}

class Album {
    String name;
}