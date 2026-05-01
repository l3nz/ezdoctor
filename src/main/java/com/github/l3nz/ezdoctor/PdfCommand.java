package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.File;

@Command(name = "pdf", description = "Convert AsciiDoc to PDF")
public class PdfCommand implements Runnable {

    @Mixin ConvertArgs args;

    @Override
    public void run() {
        if (!args.input.exists()) {
            System.err.println("File not found: " + args.input.getAbsolutePath());
            System.exit(1);
        }

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.requireLibrary("asciidoctor-diagram");

            String revnumber = args.rev ? readRevnumber(asciidoctor, args.input) : null;
            File outputFile = resolveOutput(args.input, args.output, revnumber);

            Attributes attributes = buildAttributes(args.style);

            Options options = Options.builder()
                    .safe(SafeMode.SAFE)
                    .backend("pdf")
                    .mkDirs(true)
                    .attributes(attributes)
                    .toFile(outputFile)
                    .build();

            asciidoctor.convertFile(args.input, options);
            System.out.println("Converted: " + args.input.getName() + " -> " + outputFile.getName());
        }
    }

    private static Attributes buildAttributes(String style) {
        if (style != null) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute("pdf-style", style)
                    .build();
        }
        return Attributes.builder()
                .attribute("source-highlighter", "rouge")
                .build();
    }

    /** Loads just the document header to read the revnumber attribute. */
    static String readRevnumber(Asciidoctor asciidoctor, File input) {
        Document doc = asciidoctor.loadFile(input, Options.builder().safe(SafeMode.SAFE).build());
        Object rev = doc.getAttribute("revnumber");
        return rev != null ? rev.toString() : null;
    }

    /**
     * Computes the output file path.
     * If an explicit output is given, it is used as-is.
     * Otherwise, derives the name from the input, optionally inserting the revnumber before the extension.
     */
    static File resolveOutput(File input, File explicitOutput, String revnumber) {
        if (explicitOutput != null) {
            return explicitOutput;
        }
        String base = input.getName().replaceFirst("\\.adoc$", "");
        String name = (revnumber != null) ? base + "-" + revnumber + ".pdf" : base + ".pdf";
        File parent = input.getParentFile();
        return new File(parent != null ? parent : new File("."), name);
    }
}
