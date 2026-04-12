package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import java.io.File;
import org.asciidoctor.OptionsBuilder;

/**
 * CLI wrapper around AsciidoctorJ for converting AsciiDoc to PDF.
 *
 * Usage: ezdoctor <input.adoc> [output.pdf]
 */
public class EzdoctorMain {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ezdoctor <input.adoc> [output.pdf]");
            System.exit(1);
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("File not found: " + inputFile.getAbsolutePath());
            System.exit(1);
        }

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.requireLibrary("asciidoctor-diagram");

            Attributes attributes = Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .build();

            OptionsBuilder optionsBuilder = Options.builder()
                    .safe(SafeMode.SAFE)
                    .backend("pdf")
                    .mkDirs(true)
                    .attributes(attributes);

            // Optional: explicit output file
            if (args.length >= 2) {
                optionsBuilder.toFile(new File(args[1]));
            }

            asciidoctor.convertFile(inputFile, optionsBuilder.build());

            String output = (args.length >= 2) ? args[1] : inputFile.getName().replace(".adoc", ".pdf");
            System.out.println("Converted: " + inputFile.getName() + " -> " + output);
        }
    }
}
