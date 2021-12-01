package com.windypaddy.tlinker;

import java.util.ArrayList;

public class Piece {
    public final byte[] data;
    public final ArrayList<TorrentFile> torrentFiles;
    public boolean verified;
    public final long start;
    public final long end;

    public Piece (byte[] data, long start, long end) {
        this.data = data;
        this.torrentFiles = new ArrayList<>();
        this.start = start;
        this.end = end;
        this.verified = false;
    }
}
