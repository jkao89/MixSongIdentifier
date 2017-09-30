package main.model;

import java.util.ArrayList;

public class MixResults {

    private ArrayList<Track> trackList;
    private gsonHelper gHelper;

    public MixResults() {
        trackList = new ArrayList<>();
        gHelper = new gsonHelper();
    }

    public Track addTrack(String result) {
        Track track = gHelper.toTrackObj(result);
        trackList.add(track);
        return track;
    }

    public Track getTrack(int offsetSec, int idInterval) {
        return trackList.get(offsetSec / idInterval);
    }

    private Track getLastTrack() {
        return trackList.get(trackList.size());
    }

    public int getLastIndex() {
        return trackList.size();
    }

    public String getLastTrackID() {
        if (!trackList.isEmpty()) {
            return getLastTrack().getACRid();
        } else {
            return "";
        }
    }

}
