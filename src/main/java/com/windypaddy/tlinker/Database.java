package com.windypaddy.tlinker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;

public class Database implements Closeable {
    private final Connection connection;
    private Path mainRepo;
    private Path extRepo;

    public Database (Path path) throws DatabaseException, SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(e, "H2 not found.");
        }

        connection = DriverManager.getConnection("jdbc:h2:" +
                (path==null ? Paths.get(System.getProperty("user.dir")).resolve("tlinker").toString() : path.toString())
        );

        init();
    }

    private void init () throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS config(id SMALLINT PRIMARY KEY, mainrepo CHAR(4096), extrepo CHAR(4096))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mainrepo(path CHAR(4096) PRIMARY KEY, hash CHAR(32) NOT NULL, size BIGINT, mtime BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS extrepo(path CHAR(4096) PRIMARY KEY, hash CHAR(32) NOT NULL, size BIGINT, mtime BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS torrents(thash CHAR(32) NOT NULL, path CHAR(4096) NOT NULL, hash CHAR(32), UNIQUE (thash, path))");
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM config WHERE id = 0")) {
                if (!resultSet.next()) {
                    try (Statement statement1 = connection.createStatement()) {
                        statement1.executeUpdate("INSERT INTO config(id) VALUES (0)");
                    }
                }
            }
        }
    }

    public void check () throws SQLException, DatabaseException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM config WHERE id = 0")) {
                resultSet.next();
                String mainRepoString = resultSet.getString("mainrepo");
                String extRepoString = resultSet.getString("extrepo");
                if (mainRepoString == null) {
                    throw new DatabaseException(null, "NO_MAIN_REPO");
                } else if (extRepoString == null) {
                    throw new DatabaseException(null, "NO_EXT_REPO");
                } else {
                    mainRepo = Path.of(mainRepoString);
                    extRepo = Path.of(extRepoString);
                }
            }
        }
    }

    public void setMainRepo (Path path) throws SQLException {
        mainRepo = path;
        setConfig("mainrepo", path);
    }

    public void setExtRepo (Path path) throws SQLException {
        extRepo = path;
        setConfig("extrepo", path);
    }

    private void setConfig (String config, Path path) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE config SET " + config + " = ? WHERE id = 0")){
            statement.setString(1, path.toString());
            statement.executeUpdate();
        }
    }

    public Map<Path, Integer> updateRepo (boolean main, boolean fast) throws IOException, SQLException {
        Path root = main ? mainRepo : extRepo;
        String repoName = main ? "mainrepo" : "extrepo";
        Path[] paths = Files.walk(root).filter(Files::isRegularFile).toArray(Path[]::new);

        HashMap<Path, Integer> updatedPaths = new HashMap<>();

        for (Path path : paths) {
            Path relativePath = root.relativize(path);
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            long size = attr.size();
            long mtime = attr.lastModifiedTime().toMillis();

            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + repoName + " WHERE path = ?")) {
                statement.setString(1, relativePath.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) { // Existed record.
                        if (fast) {
                            if (resultSet.getLong("size")!=size || resultSet.getLong("mtime")!=mtime) {
                                updateRepoRecord(repoName, relativePath, Utils.getFileMd5(path), size, mtime);
                                updatedPaths.put(relativePath, 1);
                            }
                        } else {
                            String md5 = Utils.getFileMd5(path);
                            if (!resultSet.getString("hash").equals(md5)) {
                                updateRepoRecord(repoName, relativePath, md5, size, mtime);
                                updatedPaths.put(relativePath, 1);
                            }
                        }
                    } else {
                        try (PreparedStatement statement1 = connection.prepareStatement("INSERT INTO " + repoName + "(path, hash, size, mtime) VALUES (?, ?, ?, ?)")) {
                            statement1.setString(1, relativePath.toString());
                            statement1.setString(2, Utils.getFileMd5(path));
                            statement1.setLong(3, size);
                            statement1.setLong(4, mtime);
                            statement1.executeUpdate();
                            updatedPaths.put(relativePath, 0); // New record.
                        }
                    }
                }
            }
        }

        ArrayList<Path> removedPaths = removeRepo(main, paths);
        for (Path path : removedPaths) {
            updatedPaths.put(path, 2);
        }

        return updatedPaths;
    }

    private void updateRepoRecord (String repoName, Path path, String md5, long size, long mtime) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE " + repoName + " SET hash = ?, size = ?, mtime = ? WHERE path = ?")) {
            statement.setString(4, path.toString());
            statement.setString(1, md5);
            statement.setLong(2, size);
            statement.setLong(3, mtime);
            statement.executeUpdate();
        }
    }

    private ArrayList<Path> removeRepo (boolean main, Path[] paths) throws SQLException {
        Path root = main ? mainRepo : extRepo;
        String repoName = main ? "mainrepo" : "extrepo";
        List<Path> pathList = Arrays.asList(paths);

        ArrayList<Path> removedPaths = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT path FROM " + repoName)) {
                while (resultSet.next()) {
                    String databasePathString = resultSet.getString("path");
                    Path databasePath = Path.of(databasePathString);
                    if (!pathList.contains(root.resolve(databasePath))) {
                        try (PreparedStatement statement1 = connection.prepareStatement("DELETE FROM " + repoName + " WHERE path = ?")) {
                            statement1.setString(1, databasePathString);
                            statement1.executeUpdate();
                            removedPaths.add(databasePath);
                        }
                    }
                }
            }
        }

        return removedPaths;
    }

    public void addTorrentFile (String pieceHash, Path path, String hash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO torrents(thash, path, hash) VALUES (?, ?, ?)")) {
            statement.setString(1, pieceHash);
            statement.setString(2, path.toString());
            statement.setString(3, hash);
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ignored) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE torrents SET hash = ? WHERE thash = ? AND path = ?")) {
                statement.setString(2, pieceHash);
                statement.setString(3, path.toString());
                statement.setString(1, hash);
                statement.executeUpdate();
            }
        }
    }

    public List<Path> findRepo (String pieceHash, Path path) throws SQLException {
        ArrayList<Path> paths = new ArrayList<>();
        paths.addAll(findMainRepo(pieceHash, path));
        paths.addAll(findExtRepo(pieceHash, path));
        return paths;
    }

    public List<Path> findMainRepo (String pieceHash, Path path) throws SQLException {
        return findFile(mainRepo, "mainrepo", pieceHash, path);
    }

    public List<Path> findExtRepo (String pieceHash, Path path) throws SQLException {
        return findFile(extRepo, "extrepo", pieceHash, path);
    }

    private List<Path> findFile (Path root, String repo, String pieceHash, Path path) throws SQLException {
        ArrayList<Path> paths = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT " + repo + ".path" + " AS rpath " +
                "FROM torrents INNER JOIN " + repo + " USING (hash) WHERE thash = ? AND torrents.path = ?")) {
            statement.setString(1, pieceHash);
            statement.setString(2, path.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    paths.add(root.resolve(Path.of(resultSet.getString("rpath"))));
                }
            }
        }
        return paths;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
