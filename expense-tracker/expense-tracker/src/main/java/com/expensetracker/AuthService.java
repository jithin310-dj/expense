
package com.expensetracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    // Register a new user
    public static boolean register(String username, String password)
            throws SQLException {

        username = username.trim();

        if (username.isEmpty() || password.length() < 6) {
            return false;
        }

        String hashedPassword = BCrypt.hashpw(
                password,
                BCrypt.gensalt(12)
        );

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            stmt.executeUpdate();

            return true;

        } catch (SQLException e) {

            // Username already exists
            if (e.getErrorCode() == 1062) {
                return false;
            }

            throw e;
        }
    }

    // Login an existing user
    public static int login(String username, String password)
            throws SQLException {

        String sql = "SELECT id, password_hash FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    String storedHash = rs.getString("password_hash");

                    if (BCrypt.checkpw(password, storedHash)) {
                        return rs.getInt("id");
                    }
                }
            }
        }

        return -1;
    }
}
