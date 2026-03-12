package com.library.model;

import java.time.LocalDateTime;

public class Reservation {
    private long id;
    private long bookId;
    private long userId;
    private LocalDateTime queuedAt;
    private LocalDateTime notifiedAt;

    public Reservation(long id, long bookId, long userId, LocalDateTime queuedAt, LocalDateTime notifiedAt) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.queuedAt = queuedAt;
        this.notifiedAt = notifiedAt;
    }

    public Reservation(long bookId, long userId, LocalDateTime queuedAt) {
        this(0, bookId, userId, queuedAt, null);
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

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public boolean isNotified() {
        return notifiedAt != null;
    }
}

