package com.github.l3nz.ezdoctor;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;

/** Shared parameters for all conversion subcommands. */
class ConvertArgs {

    @Parameters(index = "0", paramLabel = "<input>", description = "Input .adoc file")
    File input;

    @Parameters(index = "1", arity = "0..1", paramLabel = "<output>", description = "Output file (optional)")
    File output;

    @Option(names = "--rev", description = "Embed the document's revnumber in the output filename")
    boolean rev;
}
