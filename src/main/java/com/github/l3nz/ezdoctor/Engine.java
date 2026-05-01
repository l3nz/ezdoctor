package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;

import java.io.File;

/**
 * Wraps AsciidoctorJ conversion. One Asciidoctor instance is created per convert() call.
 */
public class Engine {

    /**
     * Converts an AsciiDoc file to the given backend format.
     *
     * @param input          source .adoc file
     * @param explicitOutput explicit output path, or null to derive from input
     * @param backend        AsciidoctorJ backend name (e.g. "pdf", "html5", "epub3")
     * @param outputExt      output file extension including dot (e.g. ".pdf")
     * @param styleAttr      attribute name for the style option, or null if unsupported
     * @param styleValue     style value, or null if not set
     * @param embedRev       if true, reads :revnumber: and appends it to the output filename
     * @return the output file that was written
     */
    public static File convert(File input, File explicitOutput,
                               String backend, String outputExt,
                               String styleAttr, String styleValue,
                               boolean embedRev) {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.requireLibrary("asciidoctor-diagram");
            // We don't need to require this every time! takes a lot of time
            //asciidoctor.requireLibrary("asciidoctor-epub3");

            String revnumber = embedRev ? readRevnumber(asciidoctor, input) : null;
            File outputFile = resolveOutput(input, explicitOutput, revnumber, outputExt);

            Attributes attributes = buildAttributes(styleAttr, styleValue);

            Options options = Options.builder()
                    .safe(SafeMode.SAFE)
                    .backend(backend)
                    .mkDirs(true)
                    .attributes(attributes)
                    .toFile(outputFile)
                    .build();

            asciidoctor.convertFile(input, options);
            return outputFile;
        }
    }

    static String readRevnumber(Asciidoctor asciidoctor, File input) {
        Document doc = asciidoctor.loadFile(input, Options.builder().safe(SafeMode.SAFE).build());
        Object rev = doc.getAttribute("revnumber");
        return rev != null ? rev.toString() : null;
    }

    static File resolveOutput(File input, File explicitOutput, String revnumber, String outputExt) {
        if (explicitOutput != null) {
            return explicitOutput;
        }
        String base = input.getName().replaceFirst("\\.adoc$", "");
        String name = (revnumber != null) ? base + "-" + revnumber + outputExt : base + outputExt;
        File parent = input.getParentFile();
        return new File(parent != null ? parent : new File("."), name);
    }

    private static Attributes buildAttributes(String styleAttr, String styleValue) {
        if (styleAttr != null && styleValue != null) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute(styleAttr, styleValue)
                    .build();
        }
        return Attributes.builder()
                .attribute("source-highlighter", "rouge")
                .build();
    }
}
