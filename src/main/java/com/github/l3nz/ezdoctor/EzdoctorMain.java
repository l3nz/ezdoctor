package com.github.l3nz.ezdoctor;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "ezdoctor",
    mixinStandardHelpOptions = true,
    versionProvider = EzdoctorMain.VersionProvider.class,
    subcommands = { PdfCommand.class, HtmlCommand.class, EpubCommand.class },
    description = {
        "Convert AsciiDoc documents to PDF, HTML, or EPUB.",
        "This is version ${sys:ezdoctor.version} - See https://github.com/l3nz/ezdoctor"
    }
)
public class EzdoctorMain implements Runnable {

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{ Version.get().toDisplayString() };
        }
    }

    @Spec CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    public static void main(String[] args) {
        System.setProperty("ezdoctor.version", Version.get().version);
        System.exit(new CommandLine(new EzdoctorMain()).execute(args));
    }
}
