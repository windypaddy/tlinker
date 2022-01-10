package com.windypaddy.tlinker;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.*;


public class Main {

    public static void main (String[] args) {
        IO io = new IO();
        Utils.OS os = null;

        if (SystemUtils.IS_OS_LINUX) {
            os = Utils.OS.LINUX;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            os = Utils.OS.WINDOWS;
        } else {
            io.unsupportedOS(System.getProperty("os.name"));
            System.exit(-2);
        }

        if (args.length == 0) {
            io.commandUsage();
            System.exit(-3);
        }

        String[] parametersRaw = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (args[0]) {
                case "autolink":
                    try {
                        AutoLink autoLink = new AutoLink(new AutoLinkParameters(parametersRaw), io);
                        autoLink.start();
                        if (autoLink.isExportLink()) {
                            Utils.exportLinkCommand(autoLink.exportLink(), autoLink.isHardLink(), os);
                        }
                    } catch (TorrentPieceDataException e) {
                        io.pieceLengthError(e);
                        System.exit(-5);
                    }
                    break;
                case "link":
                {
                    Link link = new Link(new LinkParameters(parametersRaw), io);
                    link.start();
                    break;
                }
                case "tree":
                {
                    Tree tree = new Tree(new TreeParameters(parametersRaw), io);
                    tree.start();
                    break;
                }
                case "update":
                {
                    Update update = new Update(new UpdateParameters(parametersRaw), io);
                    update.start();
                    break;
                }
                case "import":
                    try {
                        Import import_ = new Import(new ImportParameters(parametersRaw), io);
                        import_.start();
                    } catch (TorrentPieceDataException e) {
                        io.pieceLengthError(e);
                        System.exit(-5);
                    }
                    break;
                case "export":
                {
                    Export export = new Export(new ExportParameters(parametersRaw), io);
                    export.start();
                    break;
                }
                default:
                    io.commandUsage();
                    System.exit(-3);
                    break;
            }
        } catch (ParseException e) {
            System.exit(-3);
        } catch (IOException e) {
            io.exception(e);
            System.exit(-6);
        } catch (FileException e) {
            io.fileException(e);
            System.exit(-7);
        } catch (SQLException | DatabaseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
