package com.windypaddy.tlinker;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateParameters {
    public final Path mainRepo;
    public final Path extRepo;
    public final boolean fast;

    public UpdateParameters(String[] parametersRaw) throws ParseException, FileException {
        Options options = new Options();
        options.addOption(Option.builder("m")
                .argName("main")
                .hasArg()
                .type(String.class)
                .desc("Root path of the main repository.")
                .build());
        options.addOption(Option.builder("e")
                .argName("ext")
                .hasArg()
                .type(String.class)
                .desc("Root path of the extra repository.")
                .build());
        options.addOption("f", "Do not check file content.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, parametersRaw);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tlinker update [-m <main>] [-e <ext>] [-f]",
                    options);
            throw e;
        }

        if (cmd.hasOption("m")) {
            Path torrentPath = Paths.get(cmd.getOptionValue("m"));
            if (torrentPath.isAbsolute()) {
                this.mainRepo = torrentPath;
            } else {
                this.mainRepo = Paths.get(System.getProperty("user.dir")).resolve(torrentPath);
            }
            Utils.testDirectoryReadable(mainRepo);
        } else {
            mainRepo = null;
        }
        if (cmd.hasOption("e")) {
            Path sourcePath = Paths.get(cmd.getOptionValue("e"));
            if (sourcePath.isAbsolute()) {
                this.extRepo = sourcePath;
            } else {
                this.extRepo = Paths.get(System.getProperty("user.dir")).resolve(sourcePath);
            }
            Utils.testDirectoryReadable(extRepo);
        } else {
            extRepo = null;
        }
        this.fast = cmd.hasOption("f");
    }
}
