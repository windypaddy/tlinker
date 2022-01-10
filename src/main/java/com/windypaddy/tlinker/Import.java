package com.windypaddy.tlinker;

import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;

public class Import {
    private final ImportParameters parameters;
    private final AutoLink autoLink;
    private final IO io;

    public Import (ImportParameters parameters_, IO io_) throws TorrentPieceDataException, IOException {
        parameters = parameters_;
        io = io_;
        autoLink = new AutoLink(new AutoLinkParameters(parameters.torrent, parameters.source, null, false, true, parameters.quiet), io);
    }

    public void start () throws IOException, FileException, ParseException, SQLException, DatabaseException {
        autoLink.start();

        Torrent torrent = autoLink.getTorrent();

        ArrayList<Path> failedFiles = new ArrayList<>();
        ArrayList<Path> deletableFiles = new ArrayList<>();
        ArrayList<Path> movableFiles = new ArrayList<>();
        ArrayList<Path> existedFiles = new ArrayList<>();

        try (Database database = new Database(null)) {
            database.check();

            for (TorrentFile torrentFile : torrent.files) {
                int id = torrent.files.indexOf(torrentFile);
                if (torrentFile.matchedFile == null) {
                    if (!database.addTorrentFile(torrent.torrentHash, id, null)) {
                        existedFiles.add(torrentFile.file.path);
                    }
                    failedFiles.add(torrentFile.file.path);
                } else {
                    if (!database.addTorrentFile(torrent.torrentHash, id,
                            Utils.getFileMd5(parameters.source.resolve(torrentFile.matchedFile.file.path)))) {
                        existedFiles.add(torrentFile.file.path);
                    }
                    if (database.findRepo(torrent.torrentHash, id).isEmpty()) {
                        movableFiles.add(torrentFile.matchedFile.file.path);
                    } else {
                        deletableFiles.add(torrentFile.matchedFile.file.path);
                    }
                }
            }
        }

        io.importAdvices();
        for (Path path : existedFiles) io.pathAdvice(3, path);
        for (Path path : failedFiles) io.pathAdvice(0, path);
        for (Path path : deletableFiles) io.pathAdvice(2, path);
        for (Path path : movableFiles) io.pathUpdate(1, path);
    }
}
