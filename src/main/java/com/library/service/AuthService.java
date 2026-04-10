package com.library.service;

import com.library.exception.LibraryException;
import com.library.model.User;
import com.library.model.UserRole;
import com.library.repository.UserRepository;
import com.library.util.PasswordHasher;

import java.util.List;

public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new LibraryException("Username and password are required");
        }
        return userRepository.findByUsername(username)
            .filter(user -> user.getPasswordHash().equals(PasswordHasher.hash(password)))
            .orElseThrow(() -> new LibraryException("Invalid username or password"));
    }

    public User register(String fullName, String username, String password, UserRole role) {
        if (fullName == null || fullName.isBlank()
            || username == null || username.isBlank()
            || password == null || password.isBlank()) {
            throw new LibraryException("All fields are required");
        }
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new LibraryException("Username already exists");
        });
        User user = new User(username, fullName, PasswordHasher.hash(password), role);
        return userRepository.save(user);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public List<User> listStudents() {
        return userRepository.findByRole(UserRole.STUDENT);
    }

    public boolean hasLibrarian() {
        return userRepository.countByRole(UserRole.LIBRARIAN) > 0;
    }
}

