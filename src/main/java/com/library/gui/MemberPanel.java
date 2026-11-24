package com.library.gui;

import com.library.dao.MemberDAO;
import com.library.model.Member;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class MemberPanel extends JPanel {

    private final MemberDAO memberDAO = new MemberDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);

    public MemberPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Show All");
        topPanel.add(new JLabel("Search (name/email):"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Email", "Phone", "Address", "Membership Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Member");
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        bottomPanel.add(addBtn);
        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> search());
        refreshBtn.addActionListener(e -> loadAll());
        addBtn.addActionListener(e -> showMemberDialog(null));
        editBtn.addActionListener(e -> {
            Member m = getSelectedMember();
            if (m == null) {
                JOptionPane.showMessageDialog(this, "Select a member first.");
                return;
            }
            showMemberDialog(m);
        });
        deleteBtn.addActionListener(e -> deleteSelectedMember());

        loadAll();
    }

    private void loadAll() {
        try {
            populateTable(memberDAO.getAllMembers());
        } catch (SQLException ex) {
            showError("Failed to load members", ex);
        }
    }

    private void search() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAll();
            return;
        }
        try {
            populateTable(memberDAO.searchMembers(keyword));
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    private void populateTable(List<Member> members) {
        tableModel.setRowCount(0);
        for (Member m : members) {
            tableModel.addRow(new Object[]{
                    m.getMemberId(), m.getFullName(), m.getEmail(), m.getPhone(),
                    m.getAddress(), m.getMembershipDate(), m.getStatus()
            });
        }
    }

    private Member getSelectedMember() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        Member m = new Member();
        m.setMemberId((int) tableModel.getValueAt(modelRow, 0));
        m.setFullName((String) tableModel.getValueAt(modelRow, 1));
        m.setEmail((String) tableModel.getValueAt(modelRow, 2));
        m.setPhone((String) tableModel.getValueAt(modelRow, 3));
        m.setAddress((String) tableModel.getValueAt(modelRow, 4));
        m.setStatus((String) tableModel.getValueAt(modelRow, 6));
        return m;
    }

    private void deleteSelectedMember() {
        Member m = getSelectedMember();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Select a member first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete member \"" + m.getFullName() + "\"? This also removes their loan history.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            memberDAO.deleteMember(m.getMemberId());
            loadAll();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    private void showMemberDialog(Member existing) {
        JTextField nameF = new JTextField(existing != null ? existing.getFullName() : "");
        JTextField emailF = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField phoneF = new JTextField(existing != null ? existing.getPhone() : "");
        JTextField addressF = new JTextField(existing != null ? existing.getAddress() : "");
        JComboBox<String> statusF = new JComboBox<>(new String[]{"ACTIVE", "SUSPENDED", "EXPIRED"});
        if (existing != null && existing.getStatus() != null) statusF.setSelectedItem(existing.getStatus());

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Full Name:")); form.add(nameF);
        form.add(new JLabel("Email:")); form.add(emailF);
        form.add(new JLabel("Phone:")); form.add(phoneF);
        form.add(new JLabel("Address:")); form.add(addressF);
        if (existing != null) {
            form.add(new JLabel("Status:")); form.add(statusF);
        }

        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add Member" : "Edit Member",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            Member m = existing != null ? existing : new Member();
            m.setFullName(nameF.getText().trim());
            m.setEmail(emailF.getText().trim());
            m.setPhone(phoneF.getText().trim());
            m.setAddress(addressF.getText().trim());
            if (existing != null) {
                m.setStatus((String) statusF.getSelectedItem());
            }

            if (m.getFullName().isEmpty() || m.getEmail().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Email are required.");
                return;
            }

            if (existing == null) {
                memberDAO.addMember(m);
            } else {
                memberDAO.updateMember(m);
            }
            loadAll();
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

    private void showError(String message, SQLException ex) {
        JOptionPane.showMessageDialog(this, message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
