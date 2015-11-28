###**stata-translate is a tool for translating Stata strings 
 using the Google Translate API**

###Overview

**stata-translate** takes advantage of Stata's Java foreign interface to
provide one-step translation of strings from within Stata.
A new command, `gtranslate`, is provided by the `gtranslate.ado` file.
The `ado` file calls a Java class that can access the Stata data model
through the foreign interface. This Java class relies on an included
additional Java class which provides access to the Google Translate REST API.

###Installation

We intend to distribute a compiled `jar` file through the 
Boston College Statistical Software Components (SSC) archive,
the main repository for Stata plugins. A copy will also be available
here. Until then, follow the instructions for compilation.

###Compilation

Clone this repository and compile using maven: `mvn install` (add
`-DskipTests` to skip testing). Copy or symlink
`stata-translate-*-jar-with-dependencies.jar` and 
`src/main/stata/gtranslate.ado` to your Stata ado path.

###Use

stata-translate is intended to be used through the `gtranslate.ado` client.
See `gtranslate.sthlp` for instructions.
Stata's `help gtranslate` command renders the markup in the `sthlp` file.

###License

stata-translate is licensed under version 2.0 of the Apache License.
The `jar` releases contain bundled dependencies redistributed under the
terms of those dependencies' licenses.