package com.library.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Simple SQLite helper that creates and migrates the embedded database.
 */
public class DatabaseManager {

    private final String jdbcUrl;

    public DatabaseManager(String fileName) {
        Path dbPath = Path.of(fileName).toAbsolutePath();
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    isbn TEXT NOT NULL UNIQUE,
                    total_copies INTEGER NOT NULL,
                    available_copies INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    issued_at TEXT NOT NULL,
                    due_date TEXT NOT NULL,
                    returned_at TEXT,
                    fine_due REAL NOT NULL DEFAULT 0,
                    fine_paid_at TEXT,
                    FOREIGN KEY(book_id) REFERENCES books(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    queued_at TEXT NOT NULL,
                    notified_at TEXT,
                    FOREIGN KEY(book_id) REFERENCES books(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);

            ensureColumnExists(connection, "loans", "fine_due", "REAL NOT NULL DEFAULT 0");
            ensureColumnExists(connection, "loans", "fine_paid_at", "TEXT");

            seedBooks(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize database schema", e);
        }
    }

    private void seedBooks(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet result = stmt.executeQuery("SELECT COUNT(1) FROM books")) {
            if (result.next() && result.getInt(1) > 0) {
                return;
            }
        }

        String[][] defaultBooks = new String[][]{
            {"Clean Code", "Robert C. Martin", "9780132350884"},
            {"Effective Java", "Joshua Bloch", "9780134685991"},
            {"The Pragmatic Programmer", "Andrew Hunt", "9780135957059"},
            {"Design Patterns", "Erich Gamma", "9780201633610"},
            {"Refactoring", "Martin Fowler", "9780201485677"},
            {"Head First Java", "Kathy Sierra", "9780596009205"}
        };

        String insertSql = """
            INSERT INTO books(title, author, isbn, total_copies, available_copies, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (String[] book : defaultBooks) {
                insert.setString(1, book[0]);
                insert.setString(2, book[1]);
                insert.setString(3, book[2]);
                insert.setInt(4, 5);
                insert.setInt(5, 5);
                insert.setString(6, LocalDateTime.now().toString());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        boolean exists = false;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement alter = connection.createStatement()) {
                alter.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            }
        }
    }

}

