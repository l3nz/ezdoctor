package com.github.l3nz.ezdoctor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "epub", description = "Convert AsciiDoc to EPUB")
public class EpubCommand implements Runnable {

    @Mixin ConvertArgs args;

    @Override
    public void run() {
        // TODO: implement EPUB conversion
        System.out.println("epub: input=" + args.input
            + " output=" + args.output
            + " style=" + args.style);
    }
}
