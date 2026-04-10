package com.library.repository;

import com.library.db.DatabaseManager;
import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookRepository {

    private final DatabaseManager databaseManager;

    public BookRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Book save(Book book) {
        String sql = """
            INSERT INTO books(title, author, isbn, total_copies, available_copies, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            statement.setString(3, book.getIsbn());
            statement.setInt(4, book.getTotalCopies());
            statement.setInt(5, book.getAvailableCopies());
            statement.setString(6, book.getCreatedAt().toString());
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                book.setId(keys.getLong(1));
            }
            return book;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save book", e);
        }
    }

    public void update(Book book) {
        String sql = """
            UPDATE books
            SET title = ?, author = ?, isbn = ?, total_copies = ?, available_copies = ?
            WHERE id = ?
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            statement.setString(3, book.getIsbn());
            statement.setInt(4, book.getTotalCopies());
            statement.setInt(5, book.getAvailableCopies());
            statement.setLong(6, book.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update book", e);
        }
    }

    public Optional<Book> findById(long id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapBook(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch book", e);
        }
    }

    public List<Book> findAll() {
        String sql = "SELECT * FROM books ORDER BY created_at DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            List<Book> books = new ArrayList<>();
            while (rs.next()) {
                books.add(mapBook(rs));
            }
            return books;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to list books", e);
        }
    }

    private Book mapBook(ResultSet rs) throws SQLException {
        return new Book(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("isbn"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies"),
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}

