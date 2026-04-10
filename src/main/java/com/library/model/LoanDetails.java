package com.library.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanDetails {
    private final long loanId;
    private final long bookId;
    private final long userId;
    private final String bookTitle;
    private final String userName;
    private final LocalDate dueDate;
    private final LocalDateTime issuedAt;
    private final boolean returned;
    private final double fineDue;

    public LoanDetails(long loanId, long bookId, long userId, String bookTitle, String userName,
                       LocalDateTime issuedAt, LocalDate dueDate, boolean returned, double fineDue) {
        this.loanId = loanId;
        this.bookId = bookId;
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.userName = userName;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.returned = returned;
        this.fineDue = fineDue;
    }

    public long getLoanId() {
        return loanId;
    }

    public long getBookId() {
        return bookId;
    }

    public long getUserId() {
        return userId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public boolean isReturned() {
        return returned;
    }

    public double getFineDue() {
        return fineDue;
    }
}

