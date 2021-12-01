package com.windypaddy.tlinker;

import java.util.HashMap;

public class SourceFile {
    public enum Status {
        UNCHECK,
        ACCEPT,
        DENY
    }

    public final FileInfo file;
    public final HashMap<Piece, Status> status;

    public SourceFile (FileInfo file, HashMap<Piece, Status> status) {
        this.file = file;
        this.status = status;
    }

    public boolean isAccept () {
        return !status.containsValue(Status.UNCHECK) && !status.containsValue(Status.DENY);
    }
}
