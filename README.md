# Library Management System (LMS) - JavaFX Desktop Application

A full-featured **Library Management System** built with **Java 21**, **JavaFX 21**, and **SQLite**. Designed for campus libraries, this desktop app streamlines book cataloging, borrowing, and administrative tasks with a modern glassmorphism UI.

## ✨ **Key Features**
- **User Authentication**: Secure login/register for Students & Librarians (password hashing)
- **Book Management** (Librarian): Add books with ISBN tracking, monitor inventory & availability
- **Loan Lifecycle**: Issue/return books, set due dates, re-issue loans, calculate/pay fines
- **Role-Based Dashboard**:
  | Students | Librarians |
  |----------|------------|
  | View catalog & issue books | Full CRUD for books/users/loans |
  | Track personal loans & fines | Manage all active loans & returns |
  | Self-service return/re-issue | User account creation |
- **Smart UX**: Real-time availability, due date pickers, fine calculations, \"Consult Librarian\" help dialog
- **Data Persistence**: ACID-compliant SQLite database with repository pattern

## 🛠️ **Tech Stack**
```
• Java 21 + Maven + Java Modules
• JavaFX (Controls/FXML) for responsive UI
• SQLite JDBC + Custom ORM-like repositories
• Custom CSS glassmorphism theme
• MVC architecture with Services/Repositories
```

## 🚀 **Run It**
```bash
mvn clean javafx:run
```
*Initial setup auto-creates `library.db` with sample schema.*

## 📈 **Production-Ready Highlights**
- **Exception handling** throughout
- **Input validation** (ISBN, copies, dates)
- **Fine calculation** (₹5/week overdue)
- **Clean separation**: Models → Repositories → Services → Views
- **Responsive tables** with filtering/sorting

**Perfect resume project showcasing enterprise Java skills, UI/UX design, database integration, and full-stack desktop app development!** ⭐

*(~2.5K LOC, fully functional, deployable today)*
