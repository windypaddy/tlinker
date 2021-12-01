package com.windypaddy.tlinker;

public class TorrentPieceDataException extends Exception{
    public final long pieceDataLength;

    public TorrentPieceDataException(long pieceDataLength) {
        this.pieceDataLength = pieceDataLength;
    }
}
