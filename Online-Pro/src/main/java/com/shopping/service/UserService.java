package com.shopping.service;

import com.shopping.model.User;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;
import javax.sql.DataSource;
import java.sql.*;

@ApplicationScoped
public class UserService {

    @Resource(lookup = "jdbc/ShoppingCartDS")
    private DataSource ds;

    public User registerUser(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        try (Connection con = ds.getConnection()) {
            // Check duplicate
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM USERS WHERE LOWER(USERNAME) = LOWER(?)")) {
                ps.setString(1, username.trim());
                ResultSet rs = ps.executeQuery();
                rs.next();
                if (rs.getLong(1) > 0) {
                    throw new IllegalArgumentException("Username is already taken.");
                }
            }
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO USERS (USERNAME, PASSWORD, ROLE) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username.trim());
                ps.setString(2, hashed);
                ps.setString(3, "USER");
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                return new User(keys.getLong(1), username.trim(), hashed, "USER");
            }
        }
    }

    public User authenticateUser(String username, String password) throws Exception {
        if (username == null || password == null) return null;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ID, USERNAME, PASSWORD, ROLE FROM USERS WHERE LOWER(USERNAME) = LOWER(?)")) {
            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("PASSWORD");
                if (BCrypt.checkpw(password, hashed)) {
                    return new User(rs.getLong("ID"), rs.getString("USERNAME"), hashed, rs.getString("ROLE"));
                }
            }
        }
        return null;
    }

    public User findById(Long id) throws Exception {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ID, USERNAME, PASSWORD, ROLE FROM USERS WHERE ID = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(rs.getLong("ID"), rs.getString("USERNAME"), rs.getString("PASSWORD"), rs.getString("ROLE"));
            }
        }
        return null;
    }
}
