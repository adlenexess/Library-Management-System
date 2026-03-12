package com.library.model;

import java.time.LocalDateTime;

public class User {
    private long id;
    private String username;
    private String fullName;
    private String passwordHash;
    private UserRole role;
    private LocalDateTime createdAt;

    public User(long id, String username, String fullName, String passwordHash, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public User(String username, String fullName, String passwordHash, UserRole role) {
        this(0, username, fullName, passwordHash, role, LocalDateTime.now());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}

