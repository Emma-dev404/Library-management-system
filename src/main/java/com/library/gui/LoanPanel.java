package com.library.gui;

import com.library.dao.LoanDAO;
import com.library.model.Loan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LoanPanel extends JPanel {

    private final LoanDAO loanDAO = new LoanDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> filterCombo =
            new JComboBox<>(new String[]{"Active Loans", "Overdue Loans", "All Loans"});

    public LoanPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Top: borrow/return form ---
        JTextField bookIdField = new JTextField(6);
        JTextField memberIdField = new JTextField(6);
        JButton borrowBtn = new JButton("Borrow Book");

        JTextField loanIdField = new JTextField(6);
        JButton returnBtn = new JButton("Return Book");

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Book ID:"));
        topPanel.add(bookIdField);
        topPanel.add(new JLabel("Member ID:"));
        topPanel.add(memberIdField);
        topPanel.add(borrowBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(new JLabel("Loan ID to return:"));
        topPanel.add(loanIdField);
        topPanel.add(returnBtn);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: table ---
        tableModel = new DefaultTableModel(
                new Object[]{"Loan ID", "Book", "Member", "Loan Date", "Due Date", "Return Date", "Status", "Fine"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom: filter ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("View:"));
        bottomPanel.add(filterCombo);
        JButton refreshBtn = new JButton("Refresh");
        bottomPanel.add(refreshBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Wiring ---
        borrowBtn.addActionListener(e -> borrow(bookIdField, memberIdField));
        returnBtn.addActionListener(e -> returnBook(loanIdField));
        refreshBtn.addActionListener(e -> loadLoans());
        filterCombo.addActionListener(e -> loadLoans());

        loadLoans();
    }

    private void borrow(JTextField bookIdField, JTextField memberIdField) {
        try {
            int bookId = Integer.parseInt(bookIdField.getText().trim());
            int memberId = Integer.parseInt(memberIdField.getText().trim());
            int loanId = loanDAO.borrowBook(bookId, memberId);
            if (loanId == -1) {
                JOptionPane.showMessageDialog(this,
                        "Could not borrow: no available copies for that book (or invalid book ID).");
            } else {
                JOptionPane.showMessageDialog(this, "Book borrowed. Loan ID: " + loanId +
                        "\nDue in 14 days.");
                bookIdField.setText("");
                memberIdField.setText("");
                loadLoans();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Book ID and Member ID must be numbers.");
        } catch (SQLException ex) {
            showError("Borrow failed", ex);
        }
    }

    private void returnBook(JTextField loanIdField) {
        try {
            int loanId = Integer.parseInt(loanIdField.getText().trim());
            double fine = loanDAO.returnBook(loanId);
            if (fine > 0) {
                JOptionPane.showMessageDialog(this,
                        String.format("Book returned. Overdue fine: $%.2f", fine));
            } else {
                JOptionPane.showMessageDialog(this, "Book returned on time. No fine.");
            }
            loanIdField.setText("");
            loadLoans();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Loan ID must be a number.");
        } catch (SQLException ex) {
            showError("Return failed", ex);
        }
    }

    private void loadLoans() {
        try {
            String filter = (String) filterCombo.getSelectedItem();
            List<Loan> loans;
            if ("Overdue Loans".equals(filter)) {
                loans = loanDAO.getOverdueLoans();
            } else if ("All Loans".equals(filter)) {
                loans = loanDAO.getAllLoans();
            } else {
                loans = loanDAO.getActiveLoans();
            }
            tableModel.setRowCount(0);
            for (Loan l : loans) {
                tableModel.addRow(new Object[]{
                        l.getLoanId(), l.getBookTitle(), l.getMemberName(),
                        l.getLoanDate(), l.getDueDate(), l.getReturnDate(),
                        l.getStatus(), String.format("$%.2f", l.getFineAmount())
                });
            }
        } catch (SQLException ex) {
            showError("Failed to load loans", ex);
        }
    }

    private void showError(String message, SQLException ex) {
        JOptionPane.showMessageDialog(this, message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
