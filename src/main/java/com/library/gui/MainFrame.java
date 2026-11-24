package com.library.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", new BookPanel());
        tabs.addTab("Members", new MemberPanel());
        tabs.addTab("Loans", new LoanPanel());

        add(tabs, BorderLayout.CENTER);

        JLabel statusBar = new JLabel("  Library Management System v1.0 — connected via JDBC");
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(statusBar, BorderLayout.SOUTH);
    }
}
