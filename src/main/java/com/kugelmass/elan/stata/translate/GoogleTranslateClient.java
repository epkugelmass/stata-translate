package com.kugelmass.elan.stata.translate;

/**
 * Created by elan on 11/10/15.
 */

import com.google.gson.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class providing an abstraction for interacting with the Google Translate API.
 */
class GoogleTranslateClient {

    private static final RestTemplate TEMPLATE = new RestTemplate();
    private static final String BASE_URL = "https://www.googleapis.com/language/translate/v2";
    private static final String TRANSLATE_SUFFIX = "?";
    private static final String LANGUAGE_SUFFIX = "/languages?";
    private static final String DETECT_SUFFIX = "/detect?";

    /**
     * Translation billing rate in dollars per character
     */
    protected static final double TRANSLATE_BILLING_RATE = 20.0 / 1000000;

    /**
     * Language detection billing rate in dollars per character
     */
    protected static final double DETECT_BILLING_RATE = 20.0 / 1000000;

    private final String apiParameter;
    private final Map<String, String> supportedLanguages;

    /**
     * This constuctor makes a single, free call to the Google Translate API
     * in order to cache the supported languages.
     *
     * @param apiToken The api token created in Google's credential system.
     */
    protected GoogleTranslateClient(String apiToken) {
        this.apiParameter = "key=" + apiToken;

        // Cache supported languages
        String result = TEMPLATE.getForObject(
                BASE_URL + LANGUAGE_SUFFIX + apiParameter + "&target=en",
                String.class);

        JsonObject o = new JsonParser().parse(result).getAsJsonObject();

        JsonArray a = o.get("data").getAsJsonObject()
                .get("languages").getAsJsonArray();

        this.supportedLanguages = new HashMap<>();

        for (JsonElement e : a)
            supportedLanguages.put(
                    e.getAsJsonObject().get("language").getAsString(),
                    e.getAsJsonObject().get("name").getAsString());

    }

    /**
     * Get the languages supported by the Google Translate API.
     *
     * This method does not make any calls to the Google Translate API. The
     * constructor caches the supported languages.
     *
     * @return A Map of iso639-1 language codes to their common English names.
     */
    protected Map<String, String> supportedLanguages() {
        return new HashMap<>(supportedLanguages);
    }

    /**
     * Check if a language is supported by the Google Translate API.
     *
     * This method does not make any calls to the Google Translate API.
     *
     * @param lang A iso639-1 language code.
     * @return True if language is supported.
     */
    protected boolean isLanguageSupported(String lang) {
        return this.supportedLanguages.containsKey(lang);
    }

    /**
     * Get the translation of the specified string from one language to another.
     * Makes a call to the Google Translate API.
     *
     * @param fromLanguage The iso639-1 code of the query's language.
     * @param toLanguage The iso639-1 code of the target language.
     * @param query The string to be translated.
     * @return A string of the Google Translate API's attempt at translating.
     */
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

    /**
     * Get the translation of the specified string to a language.
     * Google guesses the language of the query.
     * Makes a call to the Google Translate API.
     *
     * @param toLanguage The iso639-1 code of the target language.
     * @param query The string to be translated.
     * @return A TranslationDetection object: the translated query and
     *         the detected language.
     */
    protected DetectionTranslation translate(String toLanguage, String query) {

        Map<String, String> vars = new HashMap<>();
        vars.put("to", toLanguage);
        vars.put("query", query);

        String result = TEMPLATE.getForObject(
                BASE_URL + TRANSLATE_SUFFIX + apiParameter +
                        "&target={to}&q={query}",
                String.class, vars);

        // Object documentation: https://cloud.google.com/translate/v2/using_rest#WorkingResults
        JsonObject o = new JsonParser().parse(result).getAsJsonObject()
                .get("data").getAsJsonObject()
                .get("translations").getAsJsonArray()
                .get(0).getAsJsonObject();

        return new Gson().fromJson(o, DetectionTranslation.class);
    }


    /**
     * Get the translation of the specified string from one language to another.
     * Makes a call to the Google Translate API.
     *
     * @param fromLanguage The iso639-1 code of the query's language.
     * @param toLanguage The iso639-1 code of the target language.
     * @param queries The strings to be translated.
     * @return A list of strings of the Google Translate API's attempt at translating.
     *         Order is guaranteed.
     */
    protected List<String> translate(String fromLanguage,
                                     String toLanguage, List<String> queries) {

        /*
        TODO: Send a POST request so we can send a longer query
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-HTTP-Method-Override", "GET");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>(headers);
        ...
        String result = TEMPLATE.postForObject(uri, request, String.class);
        */

        UriComponentsBuilder baseBuilder =
                UriComponentsBuilder.fromUriString(BASE_URL
                        + TRANSLATE_SUFFIX + apiParameter)
                .queryParam("source", fromLanguage)
                .queryParam("target", toLanguage);

        int baseLength = baseBuilder.toUriString().length();

        List<URI> uriList = new ArrayList<>();

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        int length = baseLength;
        int queryCount = 0;
        URI uri;

        for (String q : queries) {
            // requests may not be longer than 2k chars or request that more
            // than 128 string be translated
            if (length + q.length() >= 1900 | queryCount >= 128) {
                uri = ((UriComponentsBuilder) baseBuilder.clone())
                        .queryParams(mvm)
                        .build().toUri();
                uriList.add(uri);
                mvm = new LinkedMultiValueMap<>();
                length = baseLength;
                queryCount = 0;
            }
            mvm.add("q", q);
            length += q.length() + 3; // "&q="
            queryCount++;
        }

        uri = ((UriComponentsBuilder) baseBuilder.clone())
                .queryParams(mvm)
                .build().toUri();
        uriList.add(uri);

        List<String> translations = new ArrayList<>();

        for (int i = 0; i < uriList.size(); i++) {
            String result = TEMPLATE.getForObject(uriList.get(i), String.class);
            JsonObject o = new JsonParser().parse(result).getAsJsonObject();

            JsonArray arr = o.get("data").getAsJsonObject()
                    .get("translations").getAsJsonArray();

            for (JsonElement e : arr)
                translations.add(e.getAsJsonObject().get("translatedText").getAsString());
        }

        return translations;
    }

    /**
     * Get the Google Translate API's best guess at the language of the string.
     * Makes a call to the Google Translate API.
     *
     * @param query The string whose language should be determined.
     * @return A Detection object: the detected language,
     *         Google's confidence in the detection, and the original query.
     */
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

    /**
     * A class representing the result of the Google Translate API's attempt to
     * detect the language of a string.
     */
    protected class Detection {
        public final String language;
        public final double confidence;
        public final String query;

        /**
         *
         * @param language The best-guess detected language.
         * @param confidence Confidence in the guess.
         * @param query The queried string.
         */
        public Detection(String language, double confidence, String query) {
            this.language = language;
            this.confidence = confidence;
            this.query = query;
        }
    }

    /**
     * A class representing the result of the Google Translate API's attempt to
     * detect the language of a string and translate it to a target language.
     */
    protected class DetectionTranslation {
        public final String translatedText;
        public final String detectedSourceLanguage;

        public DetectionTranslation(String translatedText,
                                    String detectedSourceLanguage) {
            this.translatedText = translatedText;
            this.detectedSourceLanguage = detectedSourceLanguage;
        }

    }

}
