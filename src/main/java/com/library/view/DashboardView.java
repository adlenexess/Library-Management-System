package com.library.view;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LoanDetails;
import com.library.model.User;
import com.library.model.UserRole;
import com.library.service.AuthService;
import com.library.service.LibraryService;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;

public class DashboardView extends BorderPane {

    private final User loggedInUser;
    private final LibraryService libraryService;
    private final AuthService authService;
    private final Runnable logoutHandler;

    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final ObservableList<LoanDetails> loans = FXCollections.observableArrayList();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<User> students = FXCollections.observableArrayList();

    public DashboardView(User loggedInUser, LibraryService libraryService, AuthService authService, Runnable logoutHandler) {
        this.loggedInUser = loggedInUser;
        this.libraryService = libraryService;
        this.authService = authService;
        this.logoutHandler = logoutHandler;
        setPadding(new Insets(24));
        getStyleClass().add("dashboard-root");
        URL css = getClass().getResource("/com/library/view/dashboard.css");
        if (css != null) {
            getStylesheets().add(css.toExternalForm());
        }

        refreshAll();

        setTop(buildHeader());
        setCenter(buildTabs());
    }

    private Node buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dashboard-header");
        Label welcome = new Label("Welcome, " + loggedInUser.getFullName() + " (" + loggedInUser.getRole() + ")");
        welcome.getStyleClass().add("headline");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = new Button("Log out");
        styleAccentButton(logoutBtn);
        logoutBtn.setOnAction(event -> {
            if (logoutHandler != null) {
                logoutHandler.run();
            }
        });
        header.getChildren().addAll(welcome, spacer, logoutBtn);
        return header;
    }

    private Node buildTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("glass-tabs");

        tabPane.getTabs().add(new Tab("Books", buildBooksTab()));

        if (loggedInUser.getRole() == UserRole.LIBRARIAN) {
            tabPane.getTabs().add(new Tab("Issue & Return", buildIssueTab()));
            tabPane.getTabs().add(new Tab("Users", buildUsersTab()));
        } else {
            tabPane.getTabs().add(new Tab("My Loans", buildStudentLoansTab()));
        }
        return tabPane;
    }

    private Node buildBooksTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(16));
        applyCardStyle(container);

        TableView<Book> bookTable = createBookTable();
        bookTable.setItems(books);

        container.getChildren().add(bookTable);

        if (loggedInUser.getRole() == UserRole.LIBRARIAN) {
            container.getChildren().add(buildAddBookForm());
        } else {
            container.getChildren().add(buildStudentIssueControl(bookTable));
        }
        return container;
    }

    private TableView<Book> createBookTable() {
        TableView<Book> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("frost-table");

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));

        TableColumn<Book, Integer> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));

        TableColumn<Book, Integer> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));

        table.getColumns().addAll(titleCol, authorCol, isbnCol, totalCol, availableCol);
        table.setPrefHeight(350);
        return table;
    }

    private Node buildAddBookForm() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);

        TextField titleField = new TextField();
        TextField authorField = new TextField();
        TextField isbnField = new TextField();
        Spinner<Integer> copiesSpinner = new Spinner<>(1, 30, 1);
        copiesSpinner.setEditable(true);

        Button saveButton = new Button("Add book");
        styleAccentButton(saveButton);
        saveButton.setOnAction(event -> {
            try {
                libraryService.addBook(
                    titleField.getText().trim(),
                    authorField.getText().trim(),
                    isbnField.getText().trim(),
                    copiesSpinner.getValue()
                );
                showAlert(Alert.AlertType.INFORMATION, "Book added", "Book was added to catalogue");
                titleField.clear();
                authorField.clear();
                isbnField.clear();
                refreshBooks();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to add book", ex.getMessage());
            }
        });

        form.addRow(0, new Label("Title"), titleField);
        form.addRow(1, new Label("Author"), authorField);
        form.addRow(2, new Label("ISBN"), isbnField);
        form.addRow(3, new Label("Copies"), copiesSpinner);
        form.add(saveButton, 1, 4);
        return form;
    }

    private Node buildStudentIssueControl(TableView<Book> bookTable) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        DatePicker duePicker = new DatePicker(LocalDate.now().plusWeeks(2));
        duePicker.getStyleClass().add("pill-control");
        Button issueButton = new Button("Issue selected");
        styleAccentButton(issueButton);
        Button returnButton = new Button("Return selected");
        styleSecondaryButton(returnButton);

        issueButton.setOnAction(event -> {
            Book selected = bookTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select book", "Pick a book to issue");
                return;
            }
            try {
                libraryService.issueBook(selected.getId(), loggedInUser.getId(), duePicker.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Issued", "Book issued to you");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to issue", ex.getMessage());
            }
        });

        returnButton.setOnAction(event -> {
            Book selected = bookTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select book", "Pick a book you want to return");
                return;
            }
            LoanDetails activeLoan = findActiveLoanForBook(selected.getId());
            if (activeLoan == null) {
                showAlert(Alert.AlertType.INFORMATION, "No active loan", "You haven't issued this book yet.");
                return;
            }
            try {
                libraryService.returnBook(activeLoan.getLoanId());
                showAlert(Alert.AlertType.INFORMATION, "Returned", selected.getTitle() + " returned successfully.");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to return", ex.getMessage());
            }
        });

        box.getChildren().addAll(new Label("Due date"), duePicker, issueButton, returnButton);
        return box;
    }

    private Node buildIssueTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(16));
        applyCardStyle(container);

        TableView<LoanDetails> loanTable = createLoanTable(true);
        loanTable.setItems(loans);

        container.getChildren().addAll(buildIssueForm(), loanTable, buildReturnControls(loanTable));
        return container;
    }

    private Node buildIssueForm() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);

        ComboBox<Book> bookCombo = new ComboBox<>(books.filtered(book -> book.getAvailableCopies() > 0));
        bookCombo.setPromptText("Select book");

        ComboBox<User> studentCombo = new ComboBox<>(students);
        studentCombo.setPromptText("Select student");

        DatePicker duePicker = new DatePicker(LocalDate.now().plusWeeks(2));

        Button issueBtn = new Button("Issue book");
        styleAccentButton(issueBtn);
        issueBtn.setOnAction(event -> {
            Book book = bookCombo.getValue();
            User student = studentCombo.getValue();
            if (book == null || student == null) {
                showAlert(Alert.AlertType.WARNING, "Missing info", "Select book and student first");
                return;
            }
            try {
                libraryService.issueBook(book.getId(), student.getId(), duePicker.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Issued", "Book issued to " + student.getFullName());
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to issue", ex.getMessage());
            }
        });

        form.addRow(0, new Label("Book"), bookCombo);
        form.addRow(1, new Label("Student"), studentCombo);
        form.addRow(2, new Label("Due date"), duePicker);
        form.add(issueBtn, 1, 3);
        return form;
    }

    private TableView<LoanDetails> createLoanTable(boolean showBorrowerColumn) {
        TableView<LoanDetails> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("frost-table");

        TableColumn<LoanDetails, String> bookCol = new TableColumn<>("Book");
        bookCol.setCellValueFactory(details -> javafx.beans.binding.Bindings.createStringBinding(details.getValue()::getBookTitle));

        TableColumn<LoanDetails, String> userCol = new TableColumn<>("Borrower");
        userCol.setCellValueFactory(details -> javafx.beans.binding.Bindings.createStringBinding(details.getValue()::getUserName));

        TableColumn<LoanDetails, String> dueCol = new TableColumn<>("Due date");
        dueCol.setCellValueFactory(details -> javafx.beans.binding.Bindings.createStringBinding(() -> details.getValue().getDueDate().toString()));

        TableColumn<LoanDetails, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(details -> javafx.beans.binding.Bindings.createStringBinding(() ->
            details.getValue().isReturned() ? "Returned" : "Borrowed"
        ));

        TableColumn<LoanDetails, String> fineCol = new TableColumn<>("Fine");
        fineCol.setCellValueFactory(details -> javafx.beans.binding.Bindings.createStringBinding(() ->
            formatCurrency(details.getValue().getFineDue())
        ));

        if (showBorrowerColumn) {
            table.getColumns().addAll(bookCol, userCol, dueCol, statusCol, fineCol);
        } else {
            table.getColumns().addAll(bookCol, dueCol, statusCol, fineCol);
        }
        table.setPrefHeight(300);
        return table;
    }

    private Node buildReturnControls(TableView<LoanDetails> loanTable) {
        DatePicker reissuePicker = createReissuePicker(loanTable);

        Button reissueButton = new Button("Re-issue");
        styleAccentButton(reissueButton);
        reissueButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                LoanDetails selected = loanTable.getSelectionModel().getSelectedItem();
                return selected == null || selected.isReturned();
            },
            loanTable.getSelectionModel().selectedItemProperty()
        ));
        reissueButton.setOnAction(event -> {
            LoanDetails selected = loanTable.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isReturned()) {
                return;
            }
            if (reissuePicker.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Missing date", "Pick a new due date first");
                return;
            }
            try {
                libraryService.reissueLoan(selected.getLoanId(), reissuePicker.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Extended", "Due date updated");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to re-issue", ex.getMessage());
            }
        });

        Button returnButton = new Button("Mark as returned");
        styleAccentButton(returnButton);
        returnButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                LoanDetails selected = loanTable.getSelectionModel().getSelectedItem();
                return selected == null || selected.isReturned();
            },
            loanTable.getSelectionModel().selectedItemProperty()
        ));
        returnButton.setOnAction(event -> {
            LoanDetails selected = loanTable.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isReturned()) {
                return;
            }
            try {
                libraryService.returnBook(selected.getLoanId());
                showAlert(Alert.AlertType.INFORMATION, "Returned", "Loan closed");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to return", ex.getMessage());
            }
        });

        HBox actionRow = new HBox(10,
            new Label("New due date"),
            reissuePicker,
            reissueButton,
            returnButton
        );
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getStyleClass().add("action-row");

        Label heading = new Label("Manage loan");
        heading.getStyleClass().add("section-heading");

        VBox wrapper = new VBox(8, heading, actionRow);
        wrapper.getStyleClass().add("action-card");
        return wrapper;
    }

    private Node buildStudentLoansTab() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(16));
        applyCardStyle(container);
        TableView<LoanDetails> table = createLoanTable(false);
        FilteredList<LoanDetails> activeLoans = new FilteredList<>(loans, loan -> !loan.isReturned());
        table.setItems(activeLoans);

        DatePicker reissuePicker = createReissuePicker(table);
        Button reissueBtn = new Button("Re-issue selected");
        styleAccentButton(reissueBtn);
        reissueBtn.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                LoanDetails selected = table.getSelectionModel().getSelectedItem();
                return selected == null || selected.isReturned();
            },
            table.getSelectionModel().selectedItemProperty()
        ));
        reissueBtn.setOnAction(event -> {
            LoanDetails selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isReturned()) {
                return;
            }
            if (reissuePicker.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Missing date", "Pick a new due date first");
                return;
            }
            try {
                libraryService.reissueLoan(selected.getLoanId(), reissuePicker.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Extended", "Due date updated");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to re-issue", ex.getMessage());
            }
        });

        Button returnBtn = new Button("Return selected");
        styleAccentButton(returnBtn);
        returnBtn.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                LoanDetails selected = table.getSelectionModel().getSelectedItem();
                return selected == null || selected.isReturned();
            },
            table.getSelectionModel().selectedItemProperty()
        ));
        returnBtn.setOnAction(event -> {
            LoanDetails selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isReturned()) {
                return;
            }
            try {
                libraryService.returnBook(selected.getLoanId());
                showAlert(Alert.AlertType.INFORMATION, "Returned", "Loan closed");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to return", ex.getMessage());
            }
        });

        Label fineInfo = new Label();
        fineInfo.getStyleClass().add("fine-label");
        updateFineInfo(fineInfo, null);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateFineInfo(fineInfo, newSel));

        Button payFineBtn = new Button("Pay fine");
        styleAccentButton(payFineBtn);
        payFineBtn.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                LoanDetails selected = table.getSelectionModel().getSelectedItem();
                return selected == null || selected.getFineDue() <= 0;
            },
            table.getSelectionModel().selectedItemProperty()
        ));
        payFineBtn.setOnAction(event -> {
            LoanDetails selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getFineDue() <= 0) {
                return;
            }
            try {
                libraryService.payFine(selected.getLoanId());
                showAlert(Alert.AlertType.INFORMATION, "Fine paid", "Thank you for clearing your dues.");
                refreshAll();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to pay fine", ex.getMessage());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionRow = new HBox(10,
            new Label("New due date"),
            reissuePicker,
            reissueBtn,
            returnBtn,
            spacer,
            fineInfo,
            payFineBtn
        );
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getStyleClass().add("action-row");

        Label heading = new Label("My loan actions");
        heading.getStyleClass().add("section-heading");
        VBox actionWrapper = new VBox(8, heading, actionRow);
        actionWrapper.getStyleClass().add("action-card");

        container.getChildren().addAll(table, actionWrapper, buildConsultLibrarianSection());
        return container;
    }

    private Node buildUsersTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(16));
        applyCardStyle(container);
        TableView<User> userTable = new TableView<>();
        userTable.setItems(users);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<User, String> nameCol = new TableColumn<>("Full name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> cell.getValue().getRole().name()));
        userTable.getColumns().addAll(nameCol, usernameCol, roleCol);

        container.getChildren().addAll(userTable, buildInlineUserForm());
        return container;
    }

    private Node buildInlineUserForm() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);

        TextField nameField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<UserRole> roleCombo = new ComboBox<>(FXCollections.observableArrayList(UserRole.values()));
        roleCombo.getSelectionModel().select(UserRole.STUDENT);

        Button createBtn = new Button("Create user");
        styleAccentButton(createBtn);
        createBtn.setOnAction(event -> {
            try {
                authService.register(
                    nameField.getText().trim(),
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    roleCombo.getValue()
                );
                showAlert(Alert.AlertType.INFORMATION, "User created", "New account ready");
                nameField.clear();
                usernameField.clear();
                passwordField.clear();
                refreshUsers();
            } catch (LibraryException ex) {
                showAlert(Alert.AlertType.ERROR, "Unable to create user", ex.getMessage());
            }
        });

        form.addRow(0, new Label("Full name"), nameField);
        form.addRow(1, new Label("Username"), usernameField);
        form.addRow(2, new Label("Password"), passwordField);
        form.addRow(3, new Label("Role"), roleCombo);
        form.add(createBtn, 1, 4);
        return form;
    }

    private void refreshAll() {
        refreshBooks();
        refreshLoans();
        refreshUsers();
    }

    private void refreshBooks() {
        books.setAll(libraryService.listBooks());
    }

    private void refreshLoans() {
        if (loggedInUser.getRole() == UserRole.LIBRARIAN) {
            loans.setAll(libraryService.listActiveLoans());
        } else {
            loans.setAll(libraryService.listLoansForUser(loggedInUser.getId()));
        }
    }

    private void refreshUsers() {
        if (loggedInUser.getRole() == UserRole.LIBRARIAN) {
            users.setAll(authService.listUsers());
            students.setAll(authService.listStudents());
        } else {
            students.setAll(authService.listStudents());
        }
    }

    private DatePicker createReissuePicker(TableView<LoanDetails> loanTable) {
        DatePicker picker = new DatePicker(LocalDate.now().plusWeeks(2));
        picker.getStyleClass().add("pill-control");
        loanTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                picker.setValue(newSelection.getDueDate().plusWeeks(2));
            }
        });
        return picker;
    }

    private Node buildConsultLibrarianSection() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10, 0, 0, 0));
        box.getStyleClass().add("consult-card");
        Label title = new Label("Need help with a loan?");
        title.getStyleClass().add("section-heading");
        Label description = new Label("If issuing or returning a book failed, contact the librarian directly.");
        description.setWrapText(true);
        description.getStyleClass().add("muted-text");
        Button consultBtn = new Button("Consult librarian");
        styleAccentButton(consultBtn);
        consultBtn.setOnAction(event -> showConsultDialog());
        box.getChildren().addAll(title, description, consultBtn);
        return box;
    }

    private void showConsultDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Consult Librarian");
        dialog.setHeaderText("Describe the issue you're facing with book issue/return.");

        ButtonType sendType = new ButtonType("Send message", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendType, ButtonType.CLOSE);

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Describe the problem in detail...");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(5);

        Label contactInfo = new Label("Librarian contact: librarian@campus.edu | +1 (555) 010-2000");
        contactInfo.setWrapText(true);

        VBox content = new VBox(10,
            new Label("Message to librarian"),
            messageArea,
            contactInfo
        );
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        Button sendButton = (Button) dialog.getDialogPane().lookupButton(sendType);
        sendButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (messageArea.getText().isBlank()) {
                event.consume();
                showAlert(Alert.AlertType.WARNING, "Message required", "Add a few details before sending.");
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == sendType) {
                showAlert(Alert.AlertType.INFORMATION, "Message sent", "A librarian will reach out shortly.");
            }
        });
    }

    private void styleAccentButton(Button button) {
        button.getStyleClass().add("accent-btn");
    }

    private void styleSecondaryButton(Button button) {
        button.getStyleClass().add("ghost-btn");
    }

    private void applyCardStyle(Region region) {
        region.getStyleClass().add("glass-card");
    }

    private String formatCurrency(double amount) {
        return "₹" + String.format("%.2f", amount);
    }

    private void updateFineInfo(Label label, LoanDetails loan) {
        double amount = loan == null ? 0 : loan.getFineDue();
        label.setText("Fine: " + formatCurrency(amount));
    }

    private LoanDetails findActiveLoanForBook(long bookId) {
        return loans.stream()
            .filter(loan -> !loan.isReturned() && loan.getBookId() == bookId)
            .findFirst()
            .orElse(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

