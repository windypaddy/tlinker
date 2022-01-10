package com.windypaddy.tlinker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Export {
    private final ExportParameters parameters;
    private final IO io;
    private final Torrent torrent;

    public Export (ExportParameters parameters_, IO io_) throws IOException {
        parameters = parameters_;
        io = io_;

        torrent = new Torrent(parameters.torrent, true);
    }

    public void start () throws IOException, SQLException, DatabaseException {
        Tree tree = new Tree(new TreeParameters(parameters.torrent, parameters.destination, false, parameters.quiet), io);
        tree.start();

        ArrayList<Path> failedFiles = new ArrayList<>();
        try (Database database = new Database(null)) {
            database.check();
            for (TorrentFile torrentFile : torrent.files) {
                List<Path> paths = database.findRepo(torrent.torrentHash, torrent.files.indexOf(torrentFile));
                if (!paths.isEmpty()) {
                    Files.createSymbolicLink(parameters.destination.resolve(torrentFile.file.path), paths.get(0));
                } else {
                    failedFiles.add(torrentFile.file.path);
                }
            }
        }

        if (!failedFiles.isEmpty()) {
            io.failedLinks();
            for (Path path : failedFiles) {
                io.path(path, true);
            }
        }
    }
}
