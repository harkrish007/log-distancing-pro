package com.shopping.service;

import com.shopping.model.CartItem;
import com.shopping.model.Product;
import com.shopping.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private DataSource ds;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CartService cartService;

    @Test
    public void testAddToCart_Success_NewItem() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 3;

        Product product = new Product("Laptop", "Developer Laptop", 999.99, "img", 10);
        product.setId(productId);

        when(productService.findById(productId)).thenReturn(product);

        Connection con = mock(Connection.class);
        when(ds.getConnection()).thenReturn(con);

        // Existing cart check returns no row
        PreparedStatement checkPs = mock(PreparedStatement.class);
        ResultSet checkRs = mock(ResultSet.class);
        when(con.prepareStatement(contains("SELECT ID, QUANTITY"))).thenReturn(checkPs);
        when(checkPs.executeQuery()).thenReturn(checkRs);
        when(checkRs.next()).thenReturn(false);

        // Insert returns generated key
        PreparedStatement insertPs = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(con.prepareStatement(contains("INSERT"), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(insertPs);
        when(insertPs.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(10L);

        CartItem added = cartService.addToCart(userId, productId, quantity);

        assertNotNull(added);
        assertEquals(product, added.getProduct());
        assertEquals(quantity, added.getQuantity());
        verify(insertPs).executeUpdate();
    }

    @Test
    public void testAddToCart_InsufficientInventory() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 15;

        Product product = new Product("Laptop", "Developer Laptop", 999.99, "img", 10);
        product.setId(productId);
        when(productService.findById(productId)).thenReturn(product);

        assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(userId, productId, quantity));
    }

    @Test
    public void testCheckout_EmptyCart() throws Exception {
        Long userId = 1L;

        Connection con = mock(Connection.class);
        when(ds.getConnection()).thenReturn(con);

        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(con.prepareStatement(contains("JOIN PRODUCTS"))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                cartService.checkout(userId));
    }
}
