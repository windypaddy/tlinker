package com.windypaddy.tlinker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileInfo {
    public final Path path;
    public final String name;
    public final long size;

    public FileInfo (Path absolutePath, Path sourceRoot) throws IOException {
        this.path = sourceRoot.relativize(absolutePath);
        this.name = absolutePath.getFileName().toString();
        BasicFileAttributes attr = Files.readAttributes(absolutePath, BasicFileAttributes.class);
        this.size = attr.size();
    }

    public FileInfo (Path path, long size) {
        this.path = path;
        this.name = path.getFileName().toString();
        this.size = size;
    }


}
