package com.windypaddy.tlinker;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ImportParameters {
    public final Path torrent;
    public final Path source;
    public final boolean quiet;

    public ImportParameters (String[] parametersRaw) throws ParseException, FileException {
        Options options = new Options();
        options.addOption(Option.builder("t")
                .argName("torrent")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to the torrent file.")
                .build());
        options.addOption(Option.builder("s")
                .argName("source")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to source.")
                .build());
        options.addOption("q", "Quiet mode, only output errors.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, parametersRaw);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tlinker import -t <torrent> -s <source> [-q]",
                    options);
            throw e;
        }

        {
            Path torrentPath = Paths.get(cmd.getOptionValue("t"));
            if (torrentPath.isAbsolute()) {
                this.torrent = torrentPath;
            } else {
                this.torrent = Paths.get(System.getProperty("user.dir")).resolve(torrentPath);
            }
            Utils.testFileReadable(torrent);
        }
        {
            Path sourcePath = Paths.get(cmd.getOptionValue("s"));
            if (sourcePath.isAbsolute()) {
                this.source = sourcePath;
            } else {
                this.source = Paths.get(System.getProperty("user.dir")).resolve(sourcePath);
            }
            Utils.testDirectoryReadable(source);
        }
        this.quiet = cmd.hasOption("q");
    }
}
