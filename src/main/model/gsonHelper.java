package main.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class gsonHelper {
    private Gson gson;

    gsonHelper() {
            gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    Track toTrackObj (String jsonResults) {
            return gson.fromJson(jsonResults, Track.class);
    }

}


