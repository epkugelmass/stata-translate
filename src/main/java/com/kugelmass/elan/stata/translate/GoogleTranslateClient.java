package com.kugelmass.elan.stata.translate;

/**
 * Created by elan on 11/10/15.
 */

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

class GoogleTranslateClient {

    private static final RestTemplate TEMPLATE = new RestTemplate();
    private static final String BASE_URL = "https://www.googleapis.com/language/translate/v2";
    private static final String TRANSLATE_SUFFIX = "?";
    private static final String LANGUAGE_SUFFIX = "/languages?";
    private static final String DETECT_SUFFIX = "/detect?";

    // Translation billing rate in dollars per character
    protected static final double TRANSLATE_BILLING_RATE = 20.0 / 1000000;

    // Language detection billing rate in dollars per character
    protected static final double DETECT_BILLING_RATE = 20.0 / 1000000;

    private final String apiParameter;

    protected GoogleTranslateClient(String apiToken) {
        this.apiParameter = "key=" + apiToken;
    }

    protected String translate(String fromLanguage, String toLanguage, String query) {

        Map<String, String> vars = new HashMap<>();
        vars.put("from", fromLanguage);
        vars.put("to", toLanguage);
        vars.put("query", query);

        String result = TEMPLATE.getForObject(
                BASE_URL + TRANSLATE_SUFFIX + apiParameter +
                        "&source={from}&target={to}&q={query}",
                String.class, vars);

        // Object documentation: https://cloud.google.com/translate/v2/using_rest#WorkingResults
        JsonObject o = new JsonParser().parse(result).getAsJsonObject();

        String translatedString = o.get("data").getAsJsonObject()
                .get("translations").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("translatedText").getAsString();

        return translatedString;
    }

    protected Detection detect(String query) {

        String result = TEMPLATE.getForObject(
                BASE_URL + DETECT_SUFFIX + apiParameter +
                        "&q={query}",
                String.class, query);

        JsonObject o = new JsonParser().parse(result).getAsJsonObject();

        // Object documentation: https://cloud.google.com/translate/v2/using_rest#WorkingResults
        o = o.get("data").getAsJsonObject()
                .get("detections").getAsJsonArray()
                .get(0).getAsJsonArray()
                .get(0).getAsJsonObject();

        o.addProperty("query", query);
        o.remove("isReliable");

        return new Gson().fromJson(o, Detection.class);
    }

    public class Detection {
        public final String language;
        public final Double confidence;
        public final String query;

        public Detection(String language, Double confidence, String query) {
            this.language = language;
            this.confidence = confidence;
            this.query = query;
        }
    }

}
