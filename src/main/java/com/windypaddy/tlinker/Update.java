package com.windypaddy.tlinker;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class Update {
    private final UpdateParameters parameters;
    private final IO io;

    public Update (UpdateParameters parameters_, IO io_) {
        parameters = parameters_;
        io = io_;
    }

    public void start () throws SQLException, IOException, DatabaseException {
        try (Database database = new Database(null)) {
            if (parameters.mainRepo != null) {
                database.setMainRepo(parameters.mainRepo);
            }
            if (parameters.extRepo != null) {
                database.setExtRepo(parameters.extRepo);
            }
            database.check();

            {
                Map<Path, Integer> updatedPaths = database.updateRepo(true, parameters.fast);
                if (!updatedPaths.isEmpty()) {
                    io.repoUpdate(true);
                    for (Map.Entry<Path, Integer> path : updatedPaths.entrySet()) {
                        io.pathUpdate(path.getValue(), path.getKey());
                    }
                }
            }
            {
                Map<Path, Integer> updatedPaths = database.updateRepo(false, parameters.fast);
                if (!updatedPaths.isEmpty()) {
                    io.repoUpdate(false);
                    for (Map.Entry<Path, Integer> path : updatedPaths.entrySet()) {
                        io.pathUpdate(path.getValue(), path.getKey());
                    }
                }
            }

        }
    }
}
