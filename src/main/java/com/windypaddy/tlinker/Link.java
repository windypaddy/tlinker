package com.windypaddy.tlinker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Link {
    private final LinkParameters parameters;
    private final IO io;
    private final ArrayList<FileInfo> files;
    private final ArrayList<Path> directories;

    public Link (LinkParameters parameters_, IO io_) throws IOException, FileException {
        parameters = parameters_;
        io = io_;

        files = new ArrayList<>();
        directories = new ArrayList<>();
        Path[] paths = Files.walk(parameters.source).toArray(Path[]::new);
        for (Path path : paths) {
            if (Files.isRegularFile(path)) {
                files.add(new FileInfo(path, parameters.source));
            } else if (Files.isDirectory(path)) {
                directories.add(parameters.destination.resolve(parameters.source.relativize(path)));
            } else {
                throw new FileException(FileException.Type.NOT_EXIST, path);
            }
        }
    }

    public void start () throws IOException {
        ArrayList<Path> failedDirectories = createDirectories();
        if (!failedDirectories.isEmpty()) {
            printFailedDirectories(failedDirectories);
        }
        ArrayList<FileInfo> failedFiles = createLinks();
        if (!failedFiles.isEmpty()) {
            printFailedLinks(failedFiles);
        }
    }

    private void printFailedDirectories (ArrayList<Path> failedDirectories) {
        io.failedDirectories();
        for (Path path : failedDirectories) {
            io.path(path, true);
        }
    }

    private void printFailedLinks (ArrayList<FileInfo> failedLinks) {
        io.failedLinks();
        for (FileInfo file : failedLinks) {
            io.path(file.path, true);
        }
    }

    private ArrayList<Path> createDirectories () {
        return Utils.createDirectories(directories);
    }

    private ArrayList<FileInfo> createLinks () throws IOException {
        ArrayList<FileInfo> failedLinks = new ArrayList<>();
        for (FileInfo file : files) {
            Path source = parameters.source.resolve(file.path);
            Path destination = parameters.destination.resolve(file.path);
            try {
                Utils.testFileNotExists(destination);
            } catch (FileException e) {
                failedLinks.add(file);
                continue;
            }
            if (parameters.hardLink) {
                Files.createLink(destination, source);
            } else {
                Files.createSymbolicLink(destination, source);
            }
        }
        return failedLinks;
    }
}
