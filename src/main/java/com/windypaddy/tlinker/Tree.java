package com.windypaddy.tlinker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Tree {
    private final TreeParameters parameters;
    private final IO io;
    private final Torrent torrent;
    private final Set<Path> directories;

    public Tree (TreeParameters parameters_, IO io_) throws IOException {
        parameters = parameters_;
        io = io_;

        torrent = new Torrent(parameters.torrent, false);

        directories = new HashSet<>();
        for (TorrentFile torrentFile : torrent.files) {
            Path path = torrentFile.file.path;
            int parts = path.getNameCount();
            for (int i=1; i<parts; i++) {
                directories.add(parameters.destination.resolve(path.subpath(0, i)));
            }
        }
    }

    public void start () throws IOException {
        ArrayList<Path> failedDirectories = createDirectories();
        if (!failedDirectories.isEmpty()) {
            printFailedDirectories(failedDirectories);
        }
        if (parameters.createFile) {
            ArrayList<Path> failedFiles = createFiles();
            if (!failedFiles.isEmpty()) {
                printFailedFiles(failedFiles);
            }
        }
    }

    private void printFailedDirectories (ArrayList<Path> failedDirectories) {
        io.failedDirectories();
        for (Path path : failedDirectories) {
            io.path(path, true);
        }
    }

    private void printFailedFiles (ArrayList<Path> failedFiles) {
        io.failedFiles();
        for (Path path : failedFiles) {
            io.path(path, true);
        }
    }

    public ArrayList<Path> createDirectories () {
        return Utils.createDirectories(new ArrayList<>(directories));
    }

    public ArrayList<Path> createFiles () throws IOException {
        ArrayList<Path> failedFiles = new ArrayList<>();
        for (TorrentFile torrentFile : torrent.files) {
            Path path = parameters.destination.resolve(torrentFile.file.path);
            if (!path.toFile().createNewFile()) {
                failedFiles.add(path);
            }
        }
        return failedFiles;
    }
}
