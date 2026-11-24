package com.library.dao;

import com.library.model.Loan;
import com.library.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double FINE_PER_DAY = 0.50;

    private final BookDAO bookDAO = new BookDAO();

    /**
     * Borrow a book: decrements available copies and creates a loan record.
     * Uses a single transaction so the two operations succeed or fail together.
     * Returns the new loan_id, or -1 if no copies are available.
     */
    public int borrowBook(int bookId, int memberId) throws SQLException {
        String insertSql = "INSERT INTO loans (book_id, member_id, loan_date, due_date, status) " +
                            "VALUES (?, ?, ?, ?, 'BORROWED')";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            boolean decremented = bookDAO.decrementAvailable(bookId, conn);
            if (!decremented) {
                conn.rollback();
                return -1; // no copies available
            }

            LocalDate today = LocalDate.now();
            LocalDate due = today.plusDays(LOAN_PERIOD_DAYS);

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setDate(3, Date.valueOf(today));
                ps.setDate(4, Date.valueOf(due));
                ps.executeUpdate();
                conn.commit();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Return a book: marks the loan RETURNED, calculates any overdue fine,
     * and increments the book's available copies. All in one transaction.
     */
    public double returnBook(int loanId) throws SQLException {
        String selectSql = "SELECT book_id, due_date FROM loans WHERE loan_id=? AND status != 'RETURNED'";
        String updateSql = "UPDATE loans SET return_date=?, status='RETURNED', fine_amount=? WHERE loan_id=?";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int bookId;
            LocalDate dueDate;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setInt(1, loanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Loan not found or already returned: " + loanId);
                    }
                    bookId = rs.getInt("book_id");
                    dueDate = rs.getDate("due_date").toLocalDate();
                }
            }

            LocalDate today = LocalDate.now();
            long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
            double fine = overdueDays > 0 ? overdueDays * FINE_PER_DAY : 0.0;

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDate(1, Date.valueOf(today));
                ps.setDouble(2, fine);
                ps.setInt(3, loanId);
                ps.executeUpdate();
            }

            bookDAO.incrementAvailable(bookId, conn);
            conn.commit();
            return fine;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public List<Loan> getAllLoans() throws SQLException {
        return runLoanQuery("SELECT l.*, b.title AS book_title, m.full_name AS member_name " +
                "FROM loans l JOIN books b ON l.book_id=b.book_id " +
                "JOIN members m ON l.member_id=m.member_id ORDER BY l.loan_date DESC", null);
    }

    public List<Loan> getActiveLoans() throws SQLException {
        return runLoanQuery("SELECT l.*, b.title AS book_title, m.full_name AS member_name " +
                "FROM loans l JOIN books b ON l.book_id=b.book_id " +
                "JOIN members m ON l.member_id=m.member_id " +
                "WHERE l.status != 'RETURNED' ORDER BY l.due_date", null);
    }

    public List<Loan> getOverdueLoans() throws SQLException {
        return runLoanQuery("SELECT l.*, b.title AS book_title, m.full_name AS member_name " +
                "FROM loans l JOIN books b ON l.book_id=b.book_id " +
                "JOIN members m ON l.member_id=m.member_id " +
                "WHERE l.status != 'RETURNED' AND l.due_date < CURDATE() ORDER BY l.due_date", null);
    }

    public List<Loan> getLoansForMember(int memberId) throws SQLException {
        return runLoanQuery("SELECT l.*, b.title AS book_title, m.full_name AS member_name " +
                "FROM loans l JOIN books b ON l.book_id=b.book_id " +
                "JOIN members m ON l.member_id=m.member_id " +
                "WHERE l.member_id=? ORDER BY l.loan_date DESC", memberId);
    }

    private List<Loan> runLoanQuery(String sql, Integer param) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan l = new Loan();
                    l.setLoanId(rs.getInt("loan_id"));
                    l.setBookId(rs.getInt("book_id"));
                    l.setMemberId(rs.getInt("member_id"));
                    l.setLoanDate(rs.getDate("loan_date"));
                    l.setDueDate(rs.getDate("due_date"));
                    l.setReturnDate(rs.getDate("return_date"));
                    l.setStatus(rs.getString("status"));
                    l.setFineAmount(rs.getDouble("fine_amount"));
                    l.setBookTitle(rs.getString("book_title"));
                    l.setMemberName(rs.getString("member_name"));
                    loans.add(l);
                }
            }
        }
        return loans;
    }
}
