package com.kugelmass.elan.stata.translate;

import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created by elan on 11/13/15.
 */
public class GoogleTranslateClientTest {

    private static String SECRET_KEY;
    private static GoogleTranslateClient CLIENT;

    @BeforeClass
    public static void getSecretKey() {

        String result = "";
        InputStream inputStream = null;

        Properties p = new Properties();
        String pFilename = "config.properties";

        try {

            inputStream = GoogleTranslateClient.class.getClassLoader()
                    .getResourceAsStream(pFilename);

            if(inputStream==null){
                System.out.println("Sorry, unable to find " + pFilename);
                return;
            }

            p.load(inputStream);
            result = p.getProperty("test-key");

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        SECRET_KEY = result;
        CLIENT = new GoogleTranslateClient(SECRET_KEY);
    }

    @org.junit.Test
    public void testTranslate() throws Exception {

        assertEquals("colegio", CLIENT.translate("en", "es", "school"));

    }

    @org.junit.Test
    public void testManyTranslate() throws Exception {

        List<String> l = Arrays.asList("school", "house", "door");

        List<String> result = CLIENT.translate("en", "es", l);

        assertEquals("colegio", result.get(0));
        assertEquals("casa", result.get(1));
        assertEquals("puerta", result.get(2));

    }
    
    @org.junit.Test
    public void testManyManyTranslate() throws Exception {
        
        List<String> l = new LinkedList<>();
        for (int i = 0; i < 450; i++) { // (4+7)*450 > 3000
            l.add("four");
        }
        l.add("five");

        List<String> result = CLIENT.translate("en", "es", l);

        for (int i = 0; i < 450; i++)
            assertEquals("cuatro", result.get(i));
        assertEquals("cinco", result.get(450));
        
    }

    @org.junit.Test
    public void testDetectTranslate() {

        GoogleTranslateClient.DetectionTranslation dt =
                CLIENT.translate("es", "five");

        assertEquals("cinco", dt.translatedText);
        assertEquals("en", dt.detectedSourceLanguage);

    }

    @org.junit.Test
    public void testDetect() throws Exception {

        GoogleTranslateClient.Detection d = CLIENT.detect("colegio");

        assertEquals("es", d.language);

    }

    @org.junit.Test
    public void testIsLanguageSupported() throws Exception {

        assertTrue(CLIENT.isLanguageSupported("en"));

    }
}
