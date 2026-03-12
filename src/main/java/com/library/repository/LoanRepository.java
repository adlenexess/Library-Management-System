package com.library.repository;

import com.library.db.DatabaseManager;
import com.library.model.Loan;
import com.library.model.LoanDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanRepository {

    private final DatabaseManager databaseManager;

    public LoanRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Loan save(Loan loan) {
        String sql = """
            INSERT INTO loans(book_id, user_id, issued_at, due_date, fine_due)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, loan.getBookId());
            statement.setLong(2, loan.getUserId());
            statement.setString(3, loan.getIssuedAt().toString());
            statement.setString(4, loan.getDueDate().toString());
            statement.setDouble(5, loan.getFineDue());
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                loan.setId(keys.getLong(1));
            }
            return loan;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save loan", e);
        }
    }

    public Optional<Loan> findById(long id) {
        String sql = "SELECT * FROM loans WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapLoan(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch loan", e);
        }
    }

    public Optional<Loan> findActiveLoanForBook(long bookId) {
        String sql = "SELECT * FROM loans WHERE book_id = ? AND returned_at IS NULL";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapLoan(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch active loan", e);
        }
    }

    public List<LoanDetails> findActiveLoanDetails() {
        String sql = """
            SELECT l.id,
                   l.book_id,
                   l.user_id,
                   b.title,
                   u.full_name,
                   l.issued_at,
                   l.due_date,
                   l.returned_at,
                   l.fine_due
            FROM loans l
            JOIN books b ON b.id = l.book_id
            JOIN users u ON u.id = l.user_id
            WHERE l.returned_at IS NULL
            ORDER BY l.issued_at DESC
            """;
        return queryLoanDetails(sql);
    }

    public List<LoanDetails> findLoanDetailsByUser(long userId) {
        String sql = """
            SELECT l.id,
                   l.book_id,
                   l.user_id,
                   b.title,
                   u.full_name,
                   l.issued_at,
                   l.due_date,
                   l.returned_at,
                   l.fine_due
            FROM loans l
            JOIN books b ON b.id = l.book_id
            JOIN users u ON u.id = l.user_id
            WHERE l.user_id = ?
            ORDER BY l.issued_at DESC
            """;
        return queryLoanDetails(sql, userId);
    }

    public void markReturned(long loanId, LocalDateTime returnedAt) {
        String sql = "UPDATE loans SET returned_at = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, returnedAt.toString());
            statement.setLong(2, loanId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update loan", e);
        }
    }

    public void updateDueDate(long loanId, LocalDate newDueDate) {
        String sql = "UPDATE loans SET due_date = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newDueDate.toString());
            statement.setLong(2, loanId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to extend loan", e);
        }
    }

    public void updateFineDue(long loanId, double newFineDue) {
        String sql = "UPDATE loans SET fine_due = ?, fine_paid_at = CASE WHEN ? > 0 THEN NULL ELSE fine_paid_at END WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, newFineDue);
            statement.setDouble(2, newFineDue);
            statement.setLong(3, loanId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update fine", e);
        }
    }

    public void markFinePaid(long loanId, LocalDateTime paidAt) {
        String sql = "UPDATE loans SET fine_due = 0, fine_paid_at = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, paidAt.toString());
            statement.setLong(2, loanId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to mark fine paid", e);
        }
    }

    private List<LoanDetails> queryLoanDetails(String sql, Object... params) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            ResultSet rs = statement.executeQuery();
            List<LoanDetails> loans = new ArrayList<>();
            while (rs.next()) {
                loans.add(mapLoanDetails(rs));
            }
            return loans;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read loan details", e);
        }
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        return new Loan(
            rs.getLong("id"),
            rs.getLong("book_id"),
            rs.getLong("user_id"),
            LocalDateTime.parse(rs.getString("issued_at")),
            LocalDate.parse(rs.getString("due_date")),
            rs.getString("returned_at") == null ? null : LocalDateTime.parse(rs.getString("returned_at")),
            rs.getDouble("fine_due"),
            rs.getString("fine_paid_at") == null ? null : LocalDateTime.parse(rs.getString("fine_paid_at"))
        );
    }

    private LoanDetails mapLoanDetails(ResultSet rs) throws SQLException {
        return new LoanDetails(
            rs.getLong("id"),
            rs.getLong("book_id"),
            rs.getLong("user_id"),
            rs.getString("title"),
            rs.getString("full_name"),
            LocalDateTime.parse(rs.getString("issued_at")),
            LocalDate.parse(rs.getString("due_date")),
            rs.getString("returned_at") != null,
            rs.getDouble("fine_due")
        );
    }
}

