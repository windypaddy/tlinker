package com.windypaddy.tlinker;

import java.nio.file.Path;

public class FileException extends Exception {
    public enum Type {
        EXIST,
        NOT_EXIST,
        NOT_READABLE,
        NOT_WRITEABLE
    }

    public final Type type;
    public final Path path;

    public FileException (Type type, Path path) {
        this.type = type;
        this.path = path;
    }
}
