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

        try (Database database = new Database(null)) {
            database.check();

            for (TorrentFile torrentFile : torrent.files) {
                if (torrentFile.matchedFile == null) {
                    database.addTorrentFile(torrent.torrentHash, torrentFile.file.path, null);
                    failedFiles.add(torrentFile.file.path);
                } else {
                    database.addTorrentFile(torrent.torrentHash, torrentFile.file.path,
                            Utils.getFileMd5(parameters.source.resolve(torrentFile.matchedFile.file.path)));
                    if (database.findRepo(torrent.torrentHash, torrentFile.file.path).isEmpty()) {
                        movableFiles.add(torrentFile.matchedFile.file.path);
                    } else {
                        deletableFiles.add(torrentFile.matchedFile.file.path);
                    }
                }
            }
        }

        io.importAdvices();
        for (Path path : failedFiles) io.pathAdvice(0, path);
        for (Path path : deletableFiles) io.pathAdvice(2, path);
        for (Path path : movableFiles) io.pathUpdate(1, path);
    }
}
