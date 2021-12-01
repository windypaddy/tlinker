package com.windypaddy.tlinker;

import java.util.ArrayList;

public class TorrentFile {

    public final FileInfo file;
    public final ArrayList<SourceFile> sourceFiles;
    public SourceFile matchedFile;
    public final long start;
    public final long end;

    public TorrentFile (FileInfo file, ArrayList<SourceFile> sourceFiles, long start, long end) {
        this.file = file;
        this.sourceFiles = sourceFiles;
        this.matchedFile = null;
        this.start = start;
        this.end = end;
    }

    public TorrentFile (FileInfo file) {
        this.file = file;
        this.sourceFiles = null;
        this.start = -1;
        this.end = -1;
    }

    public SourceFile getAcceptedFile () {
        if (sourceFiles != null) {
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile.isAccept()) {
                    return sourceFile;
                }
            }
        }
        return null;
    }

    public ArrayList<SourceFile> getAvailableSourceFiles (Piece piece) {
        if (sourceFiles != null) {
            ArrayList<SourceFile> availableSourceFiles = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile.status.get(piece) == SourceFile.Status.UNCHECK) {
                    availableSourceFiles.add(sourceFile);
                }
            }
            return availableSourceFiles;
        } else {
            return null;
        }
    }
}
