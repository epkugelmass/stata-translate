/**
 * Created by elan on 11/10/15.
 */

package com.kugelmass.elan.stata.translate;

import com.stata.sfi.Data;
import com.stata.sfi.SFIToolkit;
import com.stata.sfi.Scalar;

public class StataTranslate {

    private static final int TRANSLATE_METHOD = 1;
    private static final int DETECT_METHOD = 2;

    private StataTranslate() {}

    public static int fromLangToLang(String[] args) {

        String fromLang = args[0];
        String toLang   = args[1];



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
            if (!Data.isVarTypeStr(i)) {
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
