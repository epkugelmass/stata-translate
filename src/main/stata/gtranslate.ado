*! version 0.1
*! GTRANSLATE: String translation via the Google Translate API
*! by Elan P. Kugelmass
*!
*! Project website:
*!   https://github.com/epkugelmass/stata-translate
*! Bug reports:
*!   https://github.com/epkugelmass/stata-translate/issues

cap program drop gtranslate
{
program define gtranslate
    version 13

    local jclass "com.kugelmass.elan.stata.translate.StataTranslate"

    * List detected languages has different syntax
    if (regexm("`1'", "^languages")) {
        syntax anything(name=languages), Key(string)
        javacall `jclass' getSupportedLanguages, args("`key'")
        exit
    }
    

    syntax varlist(min=1 string) [if] [in] ///
        [, Key(string) from(string) to(string) Detect Estimate]

    * Additional syntax checks
    if ("`key'" == "" & "`estimate'" == "") {
        display as error "key option must be specified if estimate is not"
        error 198
    }
    if ("`detect'" != "" & ("`from'" != "" | "`to'" != "")) {
        display as error ///
            "The from and to options should not be specified when detect is given"
        error 198
    }

    local jargs "`key'"

    if ("`estimate'" != "") {
        if ("`detect'" != "") {
            local method "estimateDetectCost"
        }
        else {
            local method "estimateTranslateCost"
        }
    }
    else if ("`detect'" != "") {
        local method "detectLang"
    }
    else {
        local method "fromLangToLang"
        local jargs "`jargs'" "`from'" "`to'"
    }

    javacall `jclass' `method' `varlist' `if' `in', args(`jargs')

end
}
