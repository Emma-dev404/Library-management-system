package com.library.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central place for obtaining a JDBC connection.
 * Reads settings from src/main/resources/db.properties.
 */
public final class DBConnection {

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    private DBConnection() {
    }

    private static synchronized void loadProperties() {
        if (loaded) return;
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "db.properties not found on classpath. " +
                        "Make sure src/main/resources/db.properties exists.");
            }
            props.load(in);
            Class.forName(props.getProperty("db.driver"));
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read db.properties", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver class not found on classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        loadProperties();
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }
}
