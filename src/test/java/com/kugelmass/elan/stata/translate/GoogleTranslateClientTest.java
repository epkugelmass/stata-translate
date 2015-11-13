package com.kugelmass.elan.stata.translate;

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

        return;

    }
}