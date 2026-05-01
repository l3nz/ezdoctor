package com.github.l3nz.ezdoctor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "ezdoctor",
    mixinStandardHelpOptions = true,
    version = EzdoctorMain.VERSION,
    subcommands = { PdfCommand.class, HtmlCommand.class, EpubCommand.class },
    description = "Convert AsciiDoc documents to PDF, HTML, or EPUB."
)
public class EzdoctorMain implements Runnable {

    static final String VERSION = "ezdoctor 0.1.0";

    @Spec CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new EzdoctorMain()).execute(args));
    }
}
