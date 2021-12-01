package com.windypaddy.tlinker;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LinkParameters {
    public final Path source;
    public final Path destination;
    public final boolean hardLink;
    public final boolean quiet;

    public LinkParameters (String[] parametersRaw) throws FileException, ParseException {
        Options options = new Options();
        options.addOption(Option.builder("s")
                .argName("source")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to source.")
                .build());
        options.addOption(Option.builder("d")
                .argName("destination")
                .hasArg()
                .required()
                .type(String.class)
                .desc("Path to destination.")
                .build());
        options.addOption("h", "Use hard link.");
        options.addOption("q", "Quiet mode, only output errors.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, parametersRaw);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tlinker link -s <source> -d <destination> [-h]",
                    options);
            throw e;
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
        {
            Path destinationPath = Paths.get(cmd.getOptionValue("d"));
            if (destinationPath.isAbsolute()) {
                this.destination = destinationPath;
            } else {
                this.destination = Paths.get(System.getProperty("user.dir")).resolve(destinationPath);
            }
            Utils.testDirectoryReadWrite(destination);
        }
        this.hardLink = cmd.hasOption("h");
        this.quiet = cmd.hasOption("q");
    }
}
