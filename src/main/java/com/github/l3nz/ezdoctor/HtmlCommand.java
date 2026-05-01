package com.github.l3nz.ezdoctor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "html", description = "Convert AsciiDoc to HTML")
public class HtmlCommand implements Runnable {

    @Mixin ConvertArgs args;

    @Override
    public void run() {
        // TODO: implement HTML conversion
        System.out.println("html: input=" + args.input
            + " output=" + args.output
            + " style=" + args.style);
    }
}
