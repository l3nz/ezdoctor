package com.github.l3nz.ezdoctor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;

@Command(name = "pdf", description = "Convert AsciiDoc to PDF")
public class PdfCommand implements Runnable {

    @Mixin ConvertArgs args;

    @Option(names = "--theme", paramLabel = "<name>", description = "Theme name or path to a theme file (.yml)")
    String theme;

    @Override
    public void run() {
        if (!args.input.exists()) {
            System.err.println("File not found: " + args.input.getAbsolutePath());
            System.exit(1);
        }
        long start = System.nanoTime();
        File out = Engine.convert(args.input, args.output, "pdf", ".pdf", theme, args.rev);
        double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
        System.out.printf("Converted: %s -> %s (took %.1f s.)%n", args.input.getName(), out.getName(), elapsed);
    }
}
