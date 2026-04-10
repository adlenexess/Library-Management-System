package com.library.model;

public enum UserRole {
    LIBRARIAN,
    STUDENT;

    public static UserRole fromDatabase(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }
}

