package com.shopping.service;

import com.shopping.model.CartItem;
import com.shopping.model.Product;
import com.shopping.model.User;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CartService {

    @Resource(lookup = "jdbc/ShoppingCartDS")
    private DataSource ds;

    @Inject
    private ProductService productService;

    @Inject
    private UserService userService;

    public List<CartItem> getCartForUser(Long userId) throws Exception {
        List<CartItem> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ci.ID, ci.QUANTITY, " +
                     "p.ID AS PID, p.NAME, p.DESCRIPTION, p.PRICE, p.IMAGEURL, p.INVENTORY " +
                     "FROM CART_ITEMS ci JOIN PRODUCTS p ON ci.PRODUCT_ID = p.ID " +
                     "WHERE ci.USER_ID = ?")) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getLong("PID"));
                product.setName(rs.getString("NAME"));
                product.setDescription(rs.getString("DESCRIPTION"));
                product.setPrice(rs.getDouble("PRICE"));
                product.setImageUrl(rs.getString("IMAGEURL"));
                product.setInventory(rs.getInt("INVENTORY"));

                User user = new User(userId, null, null, null);
                CartItem item = new CartItem(rs.getLong("ID"), user, product, rs.getInt("QUANTITY"));
                list.add(item);
            }
        }
        return list;
    }

    public CartItem addToCart(Long userId, Long productId, int quantity) throws Exception {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

        Product product = productService.findById(productId);
        if (product == null) throw new IllegalArgumentException("Product not found.");
        if (product.getInventory() < quantity)
            throw new IllegalArgumentException("Insufficient inventory. Available: " + product.getInventory());

        try (Connection con = ds.getConnection()) {
            // Check if item already in cart
            Long existingId = null;
            int existingQty = 0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ID, QUANTITY FROM CART_ITEMS WHERE USER_ID = ? AND PRODUCT_ID = ?")) {
                ps.setLong(1, userId);
                ps.setLong(2, productId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    existingId = rs.getLong("ID");
                    existingQty = rs.getInt("QUANTITY");
                }
            }

            if (existingId != null) {
                int newQty = existingQty + quantity;
                if (product.getInventory() < newQty)
                    throw new IllegalArgumentException("Total quantity (" + newQty + ") exceeds inventory (" + product.getInventory() + ").");
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE CART_ITEMS SET QUANTITY = ? WHERE ID = ?")) {
                    ps.setInt(1, newQty);
                    ps.setLong(2, existingId);
                    ps.executeUpdate();
                }
                User user = new User(userId, null, null, null);
                return new CartItem(existingId, user, product, newQty);
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO CART_ITEMS (USER_ID, PRODUCT_ID, QUANTITY) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, productId);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    User user = new User(userId, null, null, null);
                    return new CartItem(keys.getLong(1), user, product, quantity);
                }
            }
        }
    }

    public CartItem updateQuantity(Long userId, Long productId, int quantity) throws Exception {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        if (quantity == 0) { removeFromCart(userId, productId); return null; }

        Product product = productService.findById(productId);
        if (product == null) throw new IllegalArgumentException("Product not found.");
        if (product.getInventory() < quantity)
            throw new IllegalArgumentException("Requested quantity (" + quantity + ") exceeds inventory (" + product.getInventory() + ").");

        try (Connection con = ds.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ID FROM CART_ITEMS WHERE USER_ID = ? AND PRODUCT_ID = ?")) {
                ps.setLong(1, userId);
                ps.setLong(2, productId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalArgumentException("Item not in cart.");
                long itemId = rs.getLong("ID");
                try (PreparedStatement upd = con.prepareStatement(
                        "UPDATE CART_ITEMS SET QUANTITY = ? WHERE ID = ?")) {
                    upd.setInt(1, quantity);
                    upd.setLong(2, itemId);
                    upd.executeUpdate();
                }
                User user = new User(userId, null, null, null);
                return new CartItem(itemId, user, product, quantity);
            }
        }
    }

    public void removeFromCart(Long userId, Long productId) throws Exception {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM CART_ITEMS WHERE USER_ID = ? AND PRODUCT_ID = ?")) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    public void clearCart(Long userId) throws Exception {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM CART_ITEMS WHERE USER_ID = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public void checkout(Long userId) throws Exception {
        List<CartItem> cartItems = getCartForUser(userId);
        if (cartItems.isEmpty()) throw new IllegalStateException("Cart is empty.");

        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (CartItem item : cartItems) {
                    Product product = item.getProduct();
                    if (product.getInventory() < item.getQuantity())
                        throw new IllegalStateException("Insufficient inventory for '" + product.getName() + "'.");
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE PRODUCTS SET INVENTORY = INVENTORY - ? WHERE ID = ?")) {
                        ps.setInt(1, item.getQuantity());
                        ps.setLong(2, product.getId());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM CART_ITEMS WHERE USER_ID = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
