/**
 * Created by elan on 11/10/15.
 */

package com.kugelmass.elan.stata.translate;

import com.stata.sfi.Data;
import com.stata.sfi.SFIToolkit;
import com.stata.sfi.Scalar;

public final class StataTranslate {

    private static final int TRANSLATE_METHOD = 1;
    private static final int DETECT_METHOD = 2;

    private StataTranslate() {}

    public static int fromLangToLang(String[] args) {

        String key      = args[0];
        String fromLang = args[1];
        String toLang   = args[2];
        String varfix   = args[3];

        int nobs1 = Data.getParsedIn1();
        int nobs2 = Data.getParsedIn2();

        int varcount = Data.getParsedVarCount();
        if (varcount < 1) {
            SFIToolkit.error("At least one variable must be specified\n");
            return 198;
        }

        int[] varmap = new int[varcount];
        for (int i = 1; i <= varcount; i++) {
            varmap[i-1] = Data.mapParsedVarIndex(i);
            if (!Data.isVarTypeStr(varmap[i-1])) {
                SFIToolkit.error("All variables must be strings\n");
                return 198;
            }
        }

        int[] newvarmap = new int[varcount];
        for (int var = 0; var < varcount; var++) {
            // Attempt to create the new variable for the translated string
            // If this fails, undo all of our work.
            String varname = Data.getVarName(varmap[var]);
            int varlength = Data.getStrVarWidth(varmap[var]);
            // TODO: Verify that this is a valid variable name
            int rc = Data.addVarStr(varname + varfix, varlength);
            if (rc != 0) {
                for (int i = 0; i < var; i++)
                    Data.dropVar(newvarmap[i]);
                SFIToolkit.error("Could not create variable " + varname + varfix
                        + ". It may already defined.\n");
                return 110;
            }
            newvarmap[var] = Data.getVarIndex(varname + varfix);
        }

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        if (!client.isLanguageSupported(fromLang)) {
            SFIToolkit.error(fromLang + " is not a supported language.");
            return 198;
        }
        if (!client.isLanguageSupported(toLang)) {
            SFIToolkit.error(toLang + " is not a supported language.");
            return 198;
        }

        for (int var = 0; var < varcount; var++) {
            for (int obs = nobs1; obs <= nobs2; obs++) {
                if (!Data.isParsedIfTrue(obs))
                    continue;
                String query = Data.getStr(varmap[var], obs);
                String result = client.translate(fromLang, toLang, query);
                Data.storeStr(newvarmap[var], obs, result);
            }
        }

        return 0;
    }

    public static int detectLang(String[] args) {

        String key = args[0];

        int nobs1 = Data.getParsedIn1();
        int nobs2 = Data.getParsedIn2();

        int varcount = Data.getParsedVarCount();
        if (varcount < 1) {
            SFIToolkit.error("At least one variable must be specified\n");
            return 198;
        }

        int[] varmap = new int[varcount];
        for (int i = 1; i <= varcount; i++) {
            varmap[i-1] = Data.mapParsedVarIndex(i);
            if (!Data.isVarTypeStr(varmap[i-1])) {
                SFIToolkit.error("All variables must be strings\n");
                return 198;
            }
        }

        int[] detectvarmap = new int[varcount];
        int[] reliabilityvarmap = new int[varcount];
        for (int var = 0; var < varcount; var++) {
            // Attempt to create the new variable for the translated string
            // If this fails, undo all of our work.
            String varname = Data.getVarName(varmap[var]);
            int varlength = Data.getStrVarWidth(varmap[var]);
            // TODO: Verify that this is a valid variable name
            int rc = Data.addVarStr(varname + "_detect", 2);
            rc += Data.addVarDouble(varname + "_detect");
            if (rc != 0) {
                for (int i = 0; i < var; i++) {
                    Data.dropVar(detectvarmap[i]);
                    Data.dropVar(reliabilityvarmap[i]);
                }
                SFIToolkit.error("Could not create _detect or _rel variables for "
                        + varname + ". It may already defined.\n");
                return 110;
            }
            detectvarmap[var] = Data.getVarIndex(varname + "_detect");
            reliabilityvarmap[var] = Data.getVarIndex(varname + "rel");
        }

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        for (int var = 0; var < varcount; var++) {
            for (int obs = nobs1; obs <= nobs2; obs++) {
                if (!Data.isParsedIfTrue(obs))
                    continue;
                String query = Data.getStr(varmap[var], obs);
                GoogleTranslateClient.Detection d = client.detect(query);
                Data.storeStr(detectvarmap[var], obs, d.language);
                Data.storeNum(reliabilityvarmap[var], obs, d.confidence);
            }
        }

        return 0;
    }

    public static int getSupportedLanguages(String[] args) {

        String key = args[0];

        GoogleTranslateClient client = new GoogleTranslateClient(key);

        SFIToolkit.display("The Google Translate API supports the following languages:\n\n");

        // Display table in Stata
        String format = "|%1$-10s|%2$-10s|\n";
        SFIToolkit.display(String.format(format, "Code", "Name"));
        SFIToolkit.display("____________________");

        for (String s : client.supportedLanguages().keySet())
            SFIToolkit.display(String.format(format, s, client.supportedLanguages().get(s)));

        return 0;
    }

    public static int estimateTranslateCost(String[] args) {
        return estimateCost(args, TRANSLATE_METHOD);
    }

    public static int estimateDetectCost(String[] args) {
        return estimateCost(args, DETECT_METHOD);
    }

    private static int estimateCost(String[] args, int method) {

        int nobs1 = Data.getParsedIn1();
        int nobs2 = Data.getParsedIn2();

        int varcount = Data.getParsedVarCount();
        if (varcount < 1) {
            SFIToolkit.error("At least one variable must be specified\n");
            return 198;
        }

        int[] varmap = new int[varcount];
        for (int i = 1; i <= varcount; i++) {
            varmap[i-1] = Data.mapParsedVarIndex(i);
            if (!Data.isVarTypeStr(varmap[i-1])) {
                SFIToolkit.error("All variables must be strings\n");
                return 198;
            }
        }

        int chars = 0;
        for (int var = 0; var < varcount; var++) {
            for (int obs = nobs1; obs <= nobs2; obs++) {
                if (!Data.isParsedIfTrue(obs))
                    continue;
                chars += Data.getStr(varmap[var], obs).length();
            }
        }

        double billingRate;
        switch (method) {
            case TRANSLATE_METHOD:
                billingRate = GoogleTranslateClient.TRANSLATE_BILLING_RATE;
                break;
            case DETECT_METHOD:
                billingRate = GoogleTranslateClient.DETECT_BILLING_RATE;
                break;
            default:
                throw new RuntimeException("Invalid Google Translate method selected");
        }

        double cost = chars * billingRate;

        SFIToolkit.display("The estimated cost is USD" + String.format("%.2f", cost));
        Scalar.setValue("cost", cost);

        return 0;

    }

}
