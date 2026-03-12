package com.library;

import com.library.db.DatabaseManager;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;
import com.library.service.AuthService;
import com.library.service.LibraryService;
import com.library.view.DashboardView;
import com.library.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Bootstraps the JavaFX application and wires core services together.
 */
public class LibraryApp extends Application {

    private DatabaseManager databaseManager;
    private UserRepository userRepository;
    private BookRepository bookRepository;
    private LoanRepository loanRepository;
    private AuthService authService;
    private LibraryService libraryService;

    @Override
    public void init() {
        databaseManager = new DatabaseManager("library.db");
        userRepository = new UserRepository(databaseManager);
        bookRepository = new BookRepository(databaseManager);
        loanRepository = new LoanRepository(databaseManager);
        authService = new AuthService(userRepository);
        libraryService = new LibraryService(bookRepository, loanRepository, userRepository);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Library Management System");
        showLogin(stage);
        stage.show();
    }

    private void showLogin(Stage stage) {
        LoginView loginView = new LoginView(
            authService,
            user -> showDashboard(stage, user)
        );
        Scene scene = new Scene(loginView, 1100, 720);
        stage.setScene(scene);
    }

    private void showDashboard(Stage stage, User user) {
        DashboardView dashboardView = new DashboardView(
            user,
            libraryService,
            authService,
            () -> showLogin(stage)
        );
        Scene scene = new Scene(dashboardView, 1200, 780);
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch();
    }
}

