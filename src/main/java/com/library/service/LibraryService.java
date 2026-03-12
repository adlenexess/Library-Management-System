package com.library.service;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.LoanDetails;
import com.library.model.User;
import com.library.model.UserRole;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.temporal.ChronoUnit;

public class LibraryService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public LibraryService(BookRepository bookRepository, LoanRepository loanRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
    }

    public List<Book> listBooks() {
        return bookRepository.findAll();
    }

    public Book addBook(String title, String author, String isbn, int copies) {
        if (copies <= 0) {
            throw new LibraryException("Copies must be greater than zero");
        }
        if (title == null || title.isBlank() || author == null || author.isBlank() || isbn == null || isbn.isBlank()) {
            throw new LibraryException("Title, author, and ISBN are required");
        }
        Book book = new Book(title, author, isbn, copies);
        return bookRepository.save(book);
    }

    public void issueBook(long bookId, long userId, LocalDate dueDate) {
        LocalDate targetDueDate = dueDate != null ? dueDate : LocalDate.now().plusWeeks(2);
        if (targetDueDate.isBefore(LocalDate.now())) {
            throw new LibraryException("Due date cannot be in the past");
        }
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new LibraryException("Book not found"));
        if (book.getAvailableCopies() <= 0) {
            throw new LibraryException("No copies available for this book");
        }
        User user = userRepository.findById(userId)
            .filter(u -> u.getRole() == UserRole.STUDENT)
            .orElseThrow(() -> new LibraryException("Only student accounts can borrow books"));
        Loan loan = new Loan(bookId, user.getId(), LocalDateTime.now(), targetDueDate);
        loanRepository.save(loan);
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.update(book);
    }

    public void returnBook(long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LibraryException("Loan not found"));
        if (loan.isReturned()) {
            throw new LibraryException("Loan already closed");
        }
        loanRepository.markReturned(loanId, LocalDateTime.now());
        bookRepository.findById(loan.getBookId()).ifPresent(book -> {
            int updated = Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1);
            book.setAvailableCopies(updated);
            bookRepository.update(book);
        });
    }

    public List<LoanDetails> listActiveLoans() {
        return loanRepository.findActiveLoanDetails();
    }

    public List<LoanDetails> listLoansForUser(long userId) {
        return loanRepository.findLoanDetailsByUser(userId);
    }

    public void reissueLoan(long loanId, LocalDate newDueDate) {
        if (newDueDate == null) {
            throw new LibraryException("Choose a new due date");
        }
        if (newDueDate.isBefore(LocalDate.now())) {
            throw new LibraryException("New due date cannot be in the past");
        }

        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LibraryException("Loan not found"));
        if (loan.isReturned()) {
            throw new LibraryException("Cannot re-issue a returned book");
        }
        if (!newDueDate.isAfter(loan.getDueDate())) {
            throw new LibraryException("New due date must be after the current due date");
        }
        long daysBetween = ChronoUnit.DAYS.between(loan.getDueDate(), newDueDate);
        long weeks = (long) Math.ceil(daysBetween / 7.0);
        if (weeks > 0) {
            double fineIncrement = weeks * 5.0;
            loanRepository.updateFineDue(loanId, loan.getFineDue() + fineIncrement);
        }
        loanRepository.updateDueDate(loanId, newDueDate);
    }

    public void payFine(long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LibraryException("Loan not found"));
        if (loan.getFineDue() <= 0) {
            throw new LibraryException("No outstanding fine for this loan");
        }
        loanRepository.markFinePaid(loanId, LocalDateTime.now());
    }
}

