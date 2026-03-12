package com.library.repository;

import com.library.db.DatabaseManager;
import com.library.model.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private final DatabaseManager databaseManager;

    public ReservationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations(book_id, user_id, queued_at, notified_at)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, reservation.getBookId());
            statement.setLong(2, reservation.getUserId());
            statement.setString(3, reservation.getQueuedAt().toString());
            statement.setString(4, reservation.getNotifiedAt() == null ? null : reservation.getNotifiedAt().toString());
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                reservation.setId(keys.getLong(1));
            }
            return reservation;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save reservation", e);
        }
    }

    public boolean existsActiveReservation(long bookId, long userId) {
        String sql = """
            SELECT 1 FROM reservations
            WHERE book_id = ? AND user_id = ?
            LIMIT 1
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setLong(2, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to check reservation", e);
        }
    }

    public List<Reservation> findByBook(long bookId) {
        String sql = """
            SELECT * FROM reservations
            WHERE book_id = ?
            ORDER BY queued_at ASC
            """;
        return queryReservations(sql, bookId);
    }

    public List<Reservation> findByUser(long userId) {
        String sql = """
            SELECT * FROM reservations
            WHERE user_id = ?
            ORDER BY queued_at ASC
            """;
        return queryReservations(sql, userId);
    }

    public Optional<Reservation> findOldestForBook(long bookId) {
        String sql = """
            SELECT * FROM reservations
            WHERE book_id = ?
            ORDER BY queued_at ASC
            LIMIT 1
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch reservation", e);
        }
    }

    public void delete(long reservationId) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete reservation", e);
        }
    }

    public void markNotified(long reservationId, LocalDateTime notifiedAt) {
        String sql = "UPDATE reservations SET notified_at = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, notifiedAt.toString());
            statement.setLong(2, reservationId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update reservation", e);
        }
    }

    private List<Reservation> queryReservations(String sql, Object... params) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            ResultSet rs = statement.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read reservations", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getLong("book_id"),
            rs.getLong("user_id"),
            LocalDateTime.parse(rs.getString("queued_at")),
            rs.getString("notified_at") == null ? null : LocalDateTime.parse(rs.getString("notified_at"))
        );
    }
}

