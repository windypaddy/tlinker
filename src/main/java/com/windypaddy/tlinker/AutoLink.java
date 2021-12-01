package com.windypaddy.tlinker;

import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.google.common.primitives.Longs.max;
import static com.google.common.primitives.Longs.min;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

public class AutoLink {
    private final AutoLinkParameters parameters;
    private final IO io;
    private final Torrent torrent;
    private HashMap<TorrentFile, SourceFile> combination;
    private ArrayList<TorrentFile> failedTorrentFiles;

    public AutoLink (AutoLinkParameters parameters_, IO io_) throws TorrentPieceDataException, IOException {
        parameters = parameters_;
        io = io_;
        if (parameters.quiet) {
            io.modeAutoLink();
            io.path("torrent_file", parameters.torrent, false);
            io.path("source_path", parameters.source, false);
            io.path("destination_path", parameters.destination, false);
        }

        torrent = new Torrent(parameters.torrent, parameters.source);
    }

    public void start () throws IOException, ParseException, FileException {
        generateCombination();
        verifyPieces();
        failedTorrentFiles = new ArrayList<>();
        for (TorrentFile torrentFile : torrent.files) {
            if (torrentFile.matchedFile == null) {
                failedTorrentFiles.add(torrentFile);
            }
        }
        if (!failedTorrentFiles.isEmpty()) {
            printFailedFiles();
        }
        if (!isExportLink()) {
            Tree tree = new Tree(new TreeParameters(parameters.torrent, parameters.destination, false, parameters.quiet), io);
            tree.start();
            ArrayList<TorrentFile> failedLinks = link();
            if (!failedLinks.isEmpty()) {
                printFailedLinks(failedLinks);
            }
        }
    }

    private void generateCombination () {
        combination = new HashMap<>();
        for (TorrentFile torrentFile : torrent.files) {
            if (!torrentFile.sourceFiles.isEmpty()) {
                combination.put(torrentFile, torrentFile.sourceFiles.get(0));
            }
        }
    }

    private void verifyPieces () throws IOException {
        for (int i=0; i<torrent.pieces.size(); i++) {
            Piece piece = torrent.pieces.get(i);
            if (verify(piece, combination, parameters.source)) {
                for (TorrentFile torrentFile : piece.torrentFiles) {
                    combination.get(torrentFile).status.put(piece, SourceFile.Status.ACCEPT);
                    SourceFile acceptedFile = torrentFile.getAcceptedFile();
                    if (acceptedFile != null) {
                        torrentFile.matchedFile = acceptedFile;
                        if (!parameters.quiet) {
                            io.linkInfo(torrentFile.file.path, acceptedFile.file.path);
                        }
                    }
                }
                piece.verified = true;
            } else {
                io.pieceFail(torrent.pieces.indexOf(piece));
                boolean failed = false;
                boolean haveAlternatives = false;
                for (TorrentFile torrentFile : piece.torrentFiles) {
                    ArrayList<SourceFile> availableSourceFiles = torrentFile.getAvailableSourceFiles(piece);
                    if (availableSourceFiles.size() == 0) {
                        io.path("no_source_file", torrentFile.file.path, true);
                        failed = true;
                    } else if (availableSourceFiles.size() > 1 && !failed) {
                        SourceFile newSourceFile = chooseSourceFile(torrentFile, combination.get(torrentFile), availableSourceFiles);
                        if (newSourceFile != null) {
                            combination.put(torrentFile, newSourceFile);
                            haveAlternatives = true;
                        } else {
                            failed = true;
                        }
                    }
                }
                if (!failed && haveAlternatives) {
                    i--;
                }
            }
        }
    }

    public boolean isHardLink () {
        return parameters.hardLink;
    }

    public boolean isExportLink () {
        return parameters.export;
    }

    private void printFailedFiles () {
        io.autoLinkError();
        for (TorrentFile torrentFile : failedTorrentFiles) {
            io.path(torrentFile.file.path, true);
        }
    }

    private void printFailedLinks (ArrayList<TorrentFile> failedLinks) {
        io.failedLinks();
        for (TorrentFile torrentFile : failedLinks) {
            io.path(parameters.destination.resolve(torrentFile.matchedFile.file.path), true);
        }
    }

    public ArrayList<TorrentFile> link () throws IOException {
        ArrayList<TorrentFile> failedLinks = new ArrayList<>();
        for (TorrentFile torrentFile : torrent.files) {
            if (torrentFile.matchedFile != null) {
                Path source = parameters.source.resolve(torrentFile.matchedFile.file.path);
                Path destination = parameters.destination.resolve(torrentFile.file.path);
                try {
                    Utils.testFileNotExists(destination);
                } catch (FileException e) {
                    failedLinks.add(torrentFile);
                }
                if (parameters.hardLink) {
                    Files.createLink(destination, source);
                } else {
                    Files.createSymbolicLink(destination, source);
                }
            }
        }
        return failedLinks;
    }

    public HashMap<Path, Path> exportLink () {
        HashMap<Path, Path> links = new HashMap<>();
        for (TorrentFile torrentFile : torrent.files) {
            if (torrentFile.matchedFile != null) {
                links.put(parameters.source.resolve(torrentFile.matchedFile.file.path),
                        parameters.destination.resolve(torrentFile.file.path));
            }
        }
        return links;
    }

    private static boolean verify (Piece piece, HashMap<TorrentFile, SourceFile> combination, Path sourceRoot) throws IOException {
        byte[] data = new byte[(int)(piece.end - piece.start + 1)];
        for (TorrentFile torrentFile : piece.torrentFiles) {
            if (combination.containsKey(torrentFile)) {
                try (RandomAccessFile file = new RandomAccessFile(
                        sourceRoot.resolve(combination.get(torrentFile).file.path).toFile(), "r"
                )) {
                    long startOffset = torrentFile.start - piece.start;
                    file.seek(startOffset < 0 ? -startOffset : 0);
                    file.read(data, startOffset > 0 ? (int) startOffset : 0,
                            (int) (min(torrentFile.end, piece.end) -
                                    max(torrentFile.start, piece.start) + 1));
                }
            } else {
                return false;
            }
        }
        return Arrays.equals(sha1(data), piece.data);
    }

    private SourceFile chooseSourceFile (TorrentFile torrentFile, SourceFile currentSourceFile, ArrayList<SourceFile> sourceFiles) {
        io.path("choose_source_file", torrentFile.file.path, false);
        io.chooseListRange(sourceFiles.size());
        for (int i=0; i<sourceFiles.size(); i++) {
            SourceFile sourceFile = sourceFiles.get(i);
            io.path(sourceFile==currentSourceFile ? 0 : i+1, sourceFile.file.path, false);
        }
        while (true) {
            int select = io.chooseList();
            if (select == -1) {
                return null;
            } else if (select == 0) {
                if (sourceFiles.contains(currentSourceFile)) {
                    return currentSourceFile;
                }
            } else if (select >= 1 && select <= sourceFiles.size()) {
                return sourceFiles.get(select-1);
            }
            io.chooseListRange(sourceFiles.size());
        }
    }

}
