package com.windypaddy.tlinker;

public class DatabaseException extends Exception {
    public final String message;
    public final Exception parent;

    public DatabaseException (Exception parent, String message) {
        this.parent = parent;
        this.message = message;
    }
}
