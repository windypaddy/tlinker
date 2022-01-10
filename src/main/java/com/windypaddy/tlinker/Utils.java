package com.windypaddy.tlinker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public abstract class Utils {
    public enum OS {
        LINUX,
        WINDOWS
    }

    public static void exportLinkCommand (HashMap<Path, Path> links, boolean hardLink, OS os) throws IOException {
        String fileName = "tlinker-" + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        switch (os) {
            case LINUX:
                fileName += ".sh";
                break;
            case WINDOWS:
                fileName += ".bat";
                break;
        }
        File file = Paths.get(System.getProperty("user.dir"), fileName).toFile();
        try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            switch (os) {
                case LINUX: {
                    bufferedWriter.write("#!/usr/bin/env bash");
                    bufferedWriter.newLine();
                    String linkCommand = "ln " + (hardLink ? "" : "-s ");
                    for (Map.Entry<Path, Path> entry : links.entrySet()) {
                        bufferedWriter.write(linkCommand + "\"" + entry.getKey().toString() + "\" \"" + entry.getValue().toString() + "\"");
                        bufferedWriter.newLine();
                    }
                } break;
                case WINDOWS: {
                    bufferedWriter.write("@echo off");
                    bufferedWriter.newLine();
                    String linkCommand = "mklink " + (hardLink ? "/H " : "");
                    for (Map.Entry<Path, Path> entry : links.entrySet()) {
                        bufferedWriter.write(linkCommand + "\"" + entry.getValue().toString() + "\" \"" + entry.getKey().toString() + "\"");
                        bufferedWriter.newLine();
                    }
                } break;
            }
        }
    }

    public static ArrayList<Path> createDirectories (ArrayList<Path> directories) {
        ArrayList<Path> failedDirectories = new ArrayList<>();
        ArrayList<Path> sortedDirectories = new ArrayList<>(directories);
        Collections.sort(sortedDirectories);
        for (Path path : sortedDirectories) {
            if (!path.toFile().mkdirs()) {
                failedDirectories.add(path);
            }
        }
        return failedDirectories;
    }

    public static void testFileExists (Path path) throws FileException {
        if (!Files.isRegularFile(path)) {
            throw new FileException(FileException.Type.NOT_EXIST, path);
        }
    }

    public static void testFileNotExists (Path path) throws FileException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            throw new FileException(FileException.Type.EXIST, path);
        }
    }

    public static void testFileReadable (Path path) throws FileException {
        testFileExists(path);
        if (!Files.isReadable(path)) {
            throw new FileException(FileException.Type.NOT_READABLE, path);
        }
    }

    public static void testFileReadWrite (Path path) throws FileException {
        testFileExists(path);
        testFileReadable(path);
        if (!Files.isWritable(path)) {
            throw new FileException(FileException.Type.NOT_WRITEABLE, path);
        }
    }

    public static void testDirectoryExists (Path path) throws FileException {
        if (!Files.isDirectory(path)) {
            throw new FileException(FileException.Type.NOT_EXIST, path);
        }
    }

    public static void testDirectoryNotExists (Path path) throws FileException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            throw new FileException(FileException.Type.EXIST, path);
        }
    }

    public static void testDirectoryReadable (Path path) throws FileException {
        testDirectoryExists(path);
        try (Stream<Path> ignored = Files.list(path)) {}
        catch (IOException e) {
            throw new FileException(FileException.Type.NOT_READABLE, path);
        }
    }

    public static void testDirectoryReadWrite (Path path) throws FileException {
        testDirectoryExists(path);
        testDirectoryReadable(path);
        File testFile = path.resolve("tlinker_write_test_" + UUID.randomUUID()).toFile();
        try {
            if (!testFile.createNewFile()) {
                throw new FileException(FileException.Type.NOT_WRITEABLE, path);
            }

            if (!testFile.delete()) {
                throw new FileException(FileException.Type.NOT_WRITEABLE, path);
            }
        } catch (Exception e) {
            throw new FileException(FileException.Type.NOT_WRITEABLE, path);
        }
    }

    public static String getFileMd5 (Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
        }
    }

}
