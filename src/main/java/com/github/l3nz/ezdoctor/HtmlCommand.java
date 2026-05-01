package com.github.l3nz.ezdoctor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.File;

@Command(name = "html", description = "Convert AsciiDoc to HTML")
public class HtmlCommand implements Runnable {

    @Mixin ConvertArgs args;

    @Override
    public void run() {
        if (!args.input.exists()) {
            System.err.println("File not found: " + args.input.getAbsolutePath());
            System.exit(1);
        }
        long start = System.nanoTime();
        File out = Engine.convert(args.input, args.output, "html5", ".html", "stylesheet", args.style, args.rev);
        double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
        System.out.printf("Converted: %s -> %s (took %.1f s.)%n", args.input.getName(), out.getName(), elapsed);
    }
}
