package com.windypaddy.tlinker;

import com.dampcake.bencode.BencodeInputStream;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Torrent {
    public final Map<String, Object> content;
    public final long pieceLength;
    public final ArrayList<Piece> pieces;
    public final ArrayList<TorrentFile> files;
    public final long size;

    public Torrent (Path torrentFilePath, Path sourceRoot) throws TorrentPieceDataException, IOException {
        content = readContent(torrentFilePath);
        pieceLength = (long) ((Map) content.get("info")).get("piece length");
        size = readSize();

        pieces = new ArrayList<>();
        ByteBuffer piecesData = (ByteBuffer) ((Map)content.get("info")).get("pieces");
        if (piecesData.array().length % 20 != 0) {
            throw new TorrentPieceDataException(piecesData.array().length);
        }
        long pieceNumber = piecesData.array().length / 20;
        for (long i=0; i<pieceNumber-1; i++) {
            byte[] data = new byte[20];
            piecesData.get(data);
            pieces.add(new Piece(data, i*pieceLength, (i+1)*pieceLength-1));
        }
        {
            byte[] data = new byte[20];
            piecesData.get(data);
            pieces.add(new Piece(data, (pieceNumber-1)*pieceLength, size-1));
        }

        files = new ArrayList<>();
        ArrayList<FileInfo> sourceFiles = findSourceFiles(sourceRoot);
        ArrayList<Map> torrentFilesRaw = (ArrayList<Map>) ((Map)content.get("info")).get("files");
        long position = 0;
        for (Map torrentFileRaw : torrentFilesRaw) {
            ArrayList<String> pathString = (ArrayList<String>)torrentFileRaw.get("path");
            Path path = Path.of(pathString.get(0));
            for (int i=1; i<pathString.size(); i++) {
                path = path.resolve(pathString.get(i));
            }
            long size = (long) torrentFileRaw.get("length");
            long start = position;
            long end = position+size-1;
            int startPiece = (int) (start/pieceLength);
            int endPiece = (int) (end/pieceLength);
            ArrayList<SourceFile> matchedSourceFiles = new ArrayList<>();
            for (FileInfo matchedSourceFile : FindFiles(sourceFiles, size, path)) {
                HashMap<Piece, SourceFile.Status> pieceStatus = new HashMap<>();
                for (int i=startPiece; i<=endPiece; i++) {
                    pieceStatus.put(pieces.get(i), SourceFile.Status.UNCHECK);
                }
                matchedSourceFiles.add(new SourceFile(matchedSourceFile, pieceStatus));
            }
            TorrentFile torrentFile = new TorrentFile(new FileInfo(path, size),
                    matchedSourceFiles, start, end);
            files.add(torrentFile);
            for (int i=startPiece; i<=endPiece; i++) {
                pieces.get(i).torrentFiles.add(torrentFile);
            }
            position += size;
        }
    }

    public Torrent (Path torrentFilePath) throws IOException {
        content = readContent(torrentFilePath);

        pieceLength = -1;
        size = -1;
        pieces = null;

        files = new ArrayList<>();
        ArrayList<Map> torrentFilesRaw = (ArrayList<Map>) ((Map)content.get("info")).get("files");
        for (Map torrentFileRaw : torrentFilesRaw) {
            ArrayList<String> pathString = (ArrayList<String>)torrentFileRaw.get("path");
            Path path = Path.of(pathString.get(0));
            for (int i=1; i<pathString.size(); i++) {
                path = path.resolve(pathString.get(i));
            }
            long size = (long) torrentFileRaw.get("length");
            files.add(new TorrentFile(new FileInfo(path, size)));
        }
    }

    public static ArrayList<FileInfo> findSourceFiles (Path root) throws IOException {
        ArrayList<FileInfo> sourceFiles = new ArrayList<>();
        Path[] paths = Files.walk(root).toArray(Path[]::new);
        for (Path path : paths) {
            if (Files.isRegularFile(path)) {
                sourceFiles.add(new FileInfo(path, root));
            }
        }
        return sourceFiles;
    }

    public static ArrayList<FileInfo> FindFiles (ArrayList<FileInfo> fileList, long size, Path expectedPath) {
        HashMap<FileInfo, Float> foundFiles = new HashMap<>();
        for (FileInfo file : fileList) {
            if (file.size == size) {
                foundFiles.put(file, pathSimilarity(file.path, expectedPath));
            }
        }
        return sortFiles(foundFiles);
    }

    private static float pathSimilarity (Path path1, Path path2) {
        return new LevenshteinDistance().apply(path1.toString(), path2.toString());
    }

    private static ArrayList<FileInfo> sortFiles (HashMap<FileInfo, Float> files) {
        ArrayList<FileInfo> sortedFiles = new ArrayList<>(files.keySet());
        sortedFiles.sort(Comparator.comparing(files::get));
        return sortedFiles;
    }

    private long readSize () {
        ArrayList<Map> torrentFilesRaw = (ArrayList<Map>) ((Map)content.get("info")).get("files");
        long torrentSize = 0;
        for (Map torrentFileRaw : torrentFilesRaw) {
            torrentSize += (long) torrentFileRaw.get("length");
        }
        return torrentSize;
    }

    private static Map<String, Object> readContent (Path torrentFilePath) throws IOException {
        Map<String, Object> content;
        try (FileInputStream fileInputStream = new FileInputStream(torrentFilePath.toFile());
             BencodeInputStream bencodeStream = new BencodeInputStream(fileInputStream)) {
            content = bencodeStream.readDictionary();
        }
        try (FileInputStream fileInputStream = new FileInputStream(torrentFilePath.toFile());
             BencodeInputStream bencodeStream = new BencodeInputStream(fileInputStream, StandardCharsets.UTF_8, true)) {
            Map<String, Object> contentRaw = bencodeStream.readDictionary();
            ((Map<String, Object>)content.get("info")).put("pieces", ((Map)contentRaw.get("info")).get("pieces"));
        }
        return content;
    }
}
