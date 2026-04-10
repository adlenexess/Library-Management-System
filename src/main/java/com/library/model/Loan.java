package com.library.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Loan {
    private long id;
    private long bookId;
    private long userId;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;
    private double fineDue;
    private LocalDateTime finePaidAt;

    public Loan(long id, long bookId, long userId, LocalDateTime issuedAt, LocalDate dueDate, LocalDateTime returnedAt,
                double fineDue, LocalDateTime finePaidAt) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.returnedAt = returnedAt;
        this.fineDue = fineDue;
        this.finePaidAt = finePaidAt;
    }

    public Loan(long bookId, long userId, LocalDateTime issuedAt, LocalDate dueDate) {
        this(0, bookId, userId, issuedAt, dueDate, null, 0, null);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBookId() {
        return bookId;
    }

    public long getUserId() {
        return userId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public boolean isReturned() {
        return returnedAt != null;
    }

    public double getFineDue() {
        return fineDue;
    }

    public void setFineDue(double fineDue) {
        this.fineDue = fineDue;
    }

    public LocalDateTime getFinePaidAt() {
        return finePaidAt;
    }

    public void setFinePaidAt(LocalDateTime finePaidAt) {
        this.finePaidAt = finePaidAt;
    }
}

