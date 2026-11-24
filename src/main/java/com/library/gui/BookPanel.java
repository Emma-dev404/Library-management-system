package com.library.gui;

import com.library.dao.BookDAO;
import com.library.model.Book;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BookPanel extends JPanel {

    private final BookDAO bookDAO = new BookDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);

    public BookPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Top: search bar ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Show All");
        topPanel.add(new JLabel("Search (title/author/ISBN):"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: table ---
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "ISBN", "Title", "Author", "Genre", "Publisher", "Year", "Total", "Available"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom: action buttons ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Book");
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Wiring ---
        searchBtn.addActionListener(e -> search());
        refreshBtn.addActionListener(e -> loadAllBooks());
        addBtn.addActionListener(e -> showBookDialog(null));
        editBtn.addActionListener(e -> {
            Book b = getSelectedBook();
            if (b == null) {
                JOptionPane.showMessageDialog(this, "Select a book first.");
                return;
            }
            showBookDialog(b);
        });
        deleteBtn.addActionListener(e -> deleteSelectedBook());

        loadAllBooks();
    }

    private void loadAllBooks() {
        try {
            populateTable(bookDAO.getAllBooks());
        } catch (SQLException ex) {
            showError("Failed to load books", ex);
        }
    }

    private void search() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllBooks();
            return;
        }
        try {
            populateTable(bookDAO.searchBooks(keyword));
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    private void populateTable(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                    b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getGenre(),
                    b.getPublisher(), b.getPublishYear(), b.getTotalCopies(), b.getAvailableCopies()
            });
        }
    }

    private Book getSelectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        Book b = new Book();
        b.setBookId((int) tableModel.getValueAt(modelRow, 0));
        b.setIsbn((String) tableModel.getValueAt(modelRow, 1));
        b.setTitle((String) tableModel.getValueAt(modelRow, 2));
        b.setAuthor((String) tableModel.getValueAt(modelRow, 3));
        b.setGenre((String) tableModel.getValueAt(modelRow, 4));
        b.setPublisher((String) tableModel.getValueAt(modelRow, 5));
        b.setPublishYear((int) tableModel.getValueAt(modelRow, 6));
        b.setTotalCopies((int) tableModel.getValueAt(modelRow, 7));
        b.setAvailableCopies((int) tableModel.getValueAt(modelRow, 8));
        return b;
    }

    private void deleteSelectedBook() {
        Book b = getSelectedBook();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Select a book first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + b.getTitle() + "\"? This also removes its loan history.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            bookDAO.deleteBook(b.getBookId());
            loadAllBooks();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    private void showBookDialog(Book existing) {
        JTextField isbnF = new JTextField(existing != null ? existing.getIsbn() : "");
        JTextField titleF = new JTextField(existing != null ? existing.getTitle() : "");
        JTextField authorF = new JTextField(existing != null ? existing.getAuthor() : "");
        JTextField genreF = new JTextField(existing != null ? existing.getGenre() : "");
        JTextField publisherF = new JTextField(existing != null ? existing.getPublisher() : "");
        JTextField yearF = new JTextField(existing != null ? String.valueOf(existing.getPublishYear()) : "");
        JTextField totalF = new JTextField(existing != null ? String.valueOf(existing.getTotalCopies()) : "1");
        JTextField availF = new JTextField(existing != null ? String.valueOf(existing.getAvailableCopies()) : "1");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("ISBN:")); form.add(isbnF);
        form.add(new JLabel("Title:")); form.add(titleF);
        form.add(new JLabel("Author:")); form.add(authorF);
        form.add(new JLabel("Genre:")); form.add(genreF);
        form.add(new JLabel("Publisher:")); form.add(publisherF);
        form.add(new JLabel("Publish Year:")); form.add(yearF);
        form.add(new JLabel("Total Copies:")); form.add(totalF);
        form.add(new JLabel("Available Copies:")); form.add(availF);

        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add Book" : "Edit Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            Book b = existing != null ? existing : new Book();
            b.setIsbn(isbnF.getText().trim());
            b.setTitle(titleF.getText().trim());
            b.setAuthor(authorF.getText().trim());
            b.setGenre(genreF.getText().trim());
            b.setPublisher(publisherF.getText().trim());
            b.setPublishYear(Integer.parseInt(yearF.getText().trim()));
            b.setTotalCopies(Integer.parseInt(totalF.getText().trim()));
            b.setAvailableCopies(Integer.parseInt(availF.getText().trim()));

            if (b.getTitle().isEmpty() || b.getAuthor().isEmpty() || b.getIsbn().isEmpty()) {
                JOptionPane.showMessageDialog(this, "ISBN, Title, and Author are required.");
                return;
            }

            if (existing == null) {
                bookDAO.addBook(b);
            } else {
                bookDAO.updateBook(b);
            }
            loadAllBooks();
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Year/Total/Available must be numbers.");
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

    private void showError(String message, SQLException ex) {
        JOptionPane.showMessageDialog(this, message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
