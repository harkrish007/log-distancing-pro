package com.shopping.service;

import com.shopping.model.Product;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProductService {

    @Resource(lookup = "jdbc/ShoppingCartDS")
    private DataSource ds;

    public List<Product> getAllProducts() throws Exception {
        List<Product> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ID, NAME, DESCRIPTION, PRICE, IMAGEURL, INVENTORY FROM PRODUCTS");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Product findById(Long id) throws Exception {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ID, NAME, DESCRIPTION, PRICE, IMAGEURL, INVENTORY FROM PRODUCTS WHERE ID = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public Product saveProduct(Product product) throws Exception {
        try (Connection con = ds.getConnection()) {
            if (product.getId() == null) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO PRODUCTS (NAME, DESCRIPTION, PRICE, IMAGEURL, INVENTORY) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, product.getName());
                    ps.setString(2, product.getDescription());
                    ps.setDouble(3, product.getPrice());
                    ps.setString(4, product.getImageUrl());
                    ps.setInt(5, product.getInventory());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    product.setId(keys.getLong(1));
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE PRODUCTS SET NAME=?, DESCRIPTION=?, PRICE=?, IMAGEURL=?, INVENTORY=? WHERE ID=?")) {
                    ps.setString(1, product.getName());
                    ps.setString(2, product.getDescription());
                    ps.setDouble(3, product.getPrice());
                    ps.setString(4, product.getImageUrl());
                    ps.setInt(5, product.getInventory());
                    ps.setLong(6, product.getId());
                    ps.executeUpdate();
                }
            }
        }
        return product;
    }

    public void deleteProduct(Long id) throws Exception {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM PRODUCTS WHERE ID = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("ID"));
        p.setName(rs.getString("NAME"));
        p.setDescription(rs.getString("DESCRIPTION"));
        p.setPrice(rs.getDouble("PRICE"));
        p.setImageUrl(rs.getString("IMAGEURL"));
        p.setInventory(rs.getInt("INVENTORY"));
        return p;
    }
}
