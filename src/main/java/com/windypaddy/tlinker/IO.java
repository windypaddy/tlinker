package com.windypaddy.tlinker;

import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

public class IO {
    private static final Map<String, String[]> messages = Map.ofEntries(
            Map.entry("unsupported_os", new String[]{"Unsupported operating system: "}),
            Map.entry("command_usage", new String[]{"Usage: tlinker [autolink | link | tree] [options]"}),
            Map.entry("auto_link_mode", new String[]{"Auto Link Mode"}),
            Map.entry("torrent_file", new String[]{"Torrent File: "}),
            Map.entry("source_path", new String[]{"Source Path: "}),
            Map.entry("destination_path", new String[]{"Destination Path: "}),
            Map.entry("piece_length_error", new String[]{"Hash data can not divided by 20, hash data length: "}),
            Map.entry("piece_fail", new String[]{"Piece ", " fails."}),
            Map.entry("no_source_file", new String[]{"No source file for "}),
            Map.entry("auto_link_error", new String[]{"Auto link finished with errors, following files can not be linked:"}),
            Map.entry("link_path_from", new String[]{"┌ "}),
            Map.entry("link_path_to", new String[]{"└ "}),
            Map.entry("choose_source_file", new String[]{"Choose source file for "}),
            Map.entry("list_current_item", new String[]{"*"}),
            Map.entry("list_skip", new String[]{"s"}),
            Map.entry("list_range", new String[]{"Please input number 1~", ", input s to skip this block, input empty to select current."}),
            Map.entry("failed_directories", new String[]{"Following directories can not be created, they may already existed."}),
            Map.entry("failed_files", new String[]{"Following files can not be created, they may already existed."}),
            Map.entry("failed_links", new String[]{"Following links can not be created, they may already existed."}),
            Map.entry("file_exist", new String[]{"Target already existed: "}),
            Map.entry("file_not_exist", new String[]{"Target not exist: "}),
            Map.entry("file_not_readable", new String[]{"Target not readable: "}),
            Map.entry("file_not_writable", new String[]{"Target not writable: "}),
            Map.entry("repo_update", new String[]{"Main repository updated:", "Extra repository updated:"}),
            Map.entry("repo_file_update", new String[]{"+", "M", "-"}),
            Map.entry("import_advice", new String[]{"Import advices:", "F Failed to import", "+ Can be moved to extra repository", "- Can be deleted from source folder", "E Already existed in the database"}),
            Map.entry("import_file_advice", new String[]{"F", "+", "-", "E"})
    );

    public IO() {
    }

    public void unsupportedOS (String OSName) {
        System.err.println(messages.get("unsupported_os")[0] + OSName);
    }

    public void exception (Exception e) {
        e.printStackTrace();
    }

    public void commandUsage () {
        System.err.println(messages.get("command_usage")[0]);
    }

    public void modeAutoLink () {
        System.out.println(messages.get("auto_link_mode")[0]);
    }

    public void repoUpdate (boolean main) {
        System.out.println(messages.get("repo_update")[main?0:1]);
    }

    public void path (String message, Path path, boolean error) {
        if (error) {
            System.err.println(messages.get(message)[0] + path);
        } else {
            System.out.println(messages.get(message)[0] + path);
        }
    }

    public void path (int index, Path path, boolean error) {
        String indexString = index==0 ? messages.get("list_current_item")[0] : Integer.toString(index);
        if (error) {
            System.err.println(indexString + " " + path);
        } else {
            System.out.println(indexString + " " + path);
        }
    }

    public void path (Path path, boolean error) {
        if (error) {
            System.err.println(path);
        } else {
            System.out.println(path);
        }
    }

    public void pathUpdate (int type, Path path) {
        System.out.println(messages.get("repo_file_update")[type] + " " + path);
    }

    public void pathAdvice (int type, Path path) {
        if (type == 0) {
            System.err.println(messages.get("import_file_advice")[type] + " " + path);
        } else {
            System.out.println(messages.get("import_file_advice")[type] + " " + path);
        }
    }

    public void pieceLengthError (TorrentPieceDataException e) {
        System.err.println(messages.get("piece_length_error")[0] + e.pieceDataLength);
    }

    public void pieceFail (int pieceIndex) {
        System.err.println(messages.get("piece_fail")[0] + pieceIndex + messages.get("piece_fail")[1]);
    }

    public void autoLinkError () {
        System.err.println(messages.get("auto_link_error")[0]);
    }

    public void linkInfo (Path path1, Path path2) {
        System.out.println(messages.get("link_path_from")[0] + path1.toString());
        System.out.println(messages.get("link_path_to")[0] + path2.toString());
    }

    public int chooseList () {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        if (line.isBlank()) {
            return 0;
        } else if (line.equals(messages.get("list_skip")[0])) {
            return -1;
        } else {
            try {
                int select = Integer.parseInt(line);
                if (select >= 1) {
                    return select;
                }
            } catch (NumberFormatException ignored) { }
            return -2;
        }
    }

    public void chooseListRange (int listSize) {
        System.out.println(messages.get("list_range")[0] + listSize + messages.get("list_range")[1]);
    }

    public void failedDirectories () {
        System.err.println(messages.get("failed_directories")[0]);
    }

    public void failedFiles () {
        System.err.println(messages.get("failed_files")[0]);
    }

    public void failedLinks () {
        System.err.println(messages.get("failed_links")[0]);
    }

    public void importAdvices () {
        System.out.println(messages.get("import_advice")[0]);
        System.out.println(messages.get("import_advice")[1]);
        System.out.println(messages.get("import_advice")[2]);
        System.out.println(messages.get("import_advice")[3]);
        System.out.println(messages.get("import_advice")[4]);
        System.out.println();
    }

    public void fileException (FileException e) {
        switch (e.type) {
            case EXIST:
                System.err.println(messages.get("file_exist")[0] + e.path);
                break;
            case NOT_EXIST:
                System.err.println(messages.get("file_not_exist")[0] + e.path);
                break;
            case NOT_READABLE:
                System.err.println(messages.get("file_not_readable")[0] + e.path);
                break;
            case NOT_WRITEABLE:
                System.err.println(messages.get("file_not_writable")[0] + e.path);
                break;
        }
    }
}
