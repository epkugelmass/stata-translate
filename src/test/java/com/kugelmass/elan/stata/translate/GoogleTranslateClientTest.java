package com.kugelmass.elan.stata.translate;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by elan on 11/13/15.
 */
public class GoogleTranslateClientTest {

    @org.junit.Test
    public void testTranslate() throws Exception {

        String key = Settings.SECRET_KEY;

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        assertEquals("colegio", client.translate("en", "es", "school"));

    }

    @org.junit.Test
    public void testManyTranslate() throws Exception {

        String key = Settings.SECRET_KEY;

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        List<String> l = Arrays.asList("school", "house", "door");

        List<String> result = client.translate("en", "es", l);

        assertEquals("colegio", result.get(0));
        assertEquals("casa", result.get(1));
        assertEquals("puerta", result.get(2));

    }

    @org.junit.Test
    public void testDetect() throws Exception {

        String key = Settings.SECRET_KEY;

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        GoogleTranslateClient.Detection d = client.detect("colegio");

        assertEquals(d.language, "es");

    }

    @org.junit.Test
    public void testIsLanguageSupported() throws Exception {

        String key = Settings.SECRET_KEY;

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        assertTrue(client.isLanguageSupported("en"));

    }
}