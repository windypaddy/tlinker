package com.windypaddy.tlinker;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TreeParameters {
    public final Path torrent;
    public final Path destination;
    public final boolean createFile;
    public final boolean quiet;

    public TreeParameters (String[] parametersRaw) throws ParseException, FileException {
        Options options = new Options();
        options.addOption(Option.builder("t")
                .argName("torrent")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to the torrent file.")
                .build());
        options.addOption(Option.builder("d")
                .argName("destination")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to destination.")
                .build());
        options.addOption("f", "Create empty file.");
        options.addOption("q", "Quiet mode, only output errors.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, parametersRaw);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tlinker tree -t <torrent> -d <destination> [-f]", options);
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
            Path destinationPath = Paths.get(cmd.getOptionValue("d"));
            if (destinationPath.isAbsolute()) {
                this.destination = destinationPath;
            } else {
                this.destination = Paths.get(System.getProperty("user.dir")).resolve(destinationPath);
            }
            Utils.testDirectoryReadWrite(destination);
        }
        this.createFile = cmd.hasOption("f");
        this.quiet = cmd.hasOption("q");
    }

    public TreeParameters (Path torrent, Path destination, boolean createFile, boolean quiet) {
        this.torrent = torrent;
        this.destination = destination;
        this.createFile = createFile;
        this.quiet = quiet;
    }
}
