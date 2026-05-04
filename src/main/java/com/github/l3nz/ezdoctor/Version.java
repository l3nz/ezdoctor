package com.github.l3nz.ezdoctor;

import java.io.InputStream;
import java.util.Properties;

/** Build metadata embedded at compile time via version.properties. */
public final class Version {

    public final String version;
    public final String gitCommit;
    public final String gitBranch;
    public final String buildTime;

    private static final Version INSTANCE = load();

    private Version(String version, String gitCommit, String gitBranch, String buildTime) {
        this.version = version;
        this.gitCommit = gitCommit;
        this.gitBranch = gitBranch;
        this.buildTime = buildTime;
    }

    public static Version get() {
        return INSTANCE;
    }

    public String toDisplayString() {
        return "ezdoctor " + version + " (" + gitCommit + "@" + gitBranch + ", built " + buildTime + ")";
    }

    private static Version load() {
        Properties p = new Properties();
        try (InputStream in = Version.class.getResourceAsStream("version.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {}
        return new Version(
            p.getProperty("version", "unknown"),
            p.getProperty("gitCommit", "unknown"),
            p.getProperty("gitBranch", "unknown"),
            p.getProperty("buildTime", "unknown")
        );
    }
}
