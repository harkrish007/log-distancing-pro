package com.shopping.service;

import com.shopping.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private DataSource ds;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private UserService userService;

    @Test
    public void testRegisterUser_Success() throws Exception {
        String username = "newuser";
        String password = "password123";

        when(ds.getConnection()).thenReturn(connection);
        // duplicate check returns count 0
        PreparedStatement countPs = mock(PreparedStatement.class);
        ResultSet countRs = mock(ResultSet.class);
        when(connection.prepareStatement(contains("COUNT"))).thenReturn(countPs);
        when(countPs.executeQuery()).thenReturn(countRs);
        when(countRs.next()).thenReturn(true);
        when(countRs.getLong(1)).thenReturn(0L);
        // insert returns generated key
        PreparedStatement insertPs = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(connection.prepareStatement(contains("INSERT"), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(insertPs);
        when(insertPs.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true);
        when(keys.getLong(1)).thenReturn(1L);

        User registered = userService.registerUser(username, password);

        assertNotNull(registered);
        assertEquals(username, registered.getUsername());
        assertEquals("USER", registered.getRole());
        assertTrue(BCrypt.checkpw(password, registered.getPassword()));
        verify(insertPs).executeUpdate();
    }

    @Test
    public void testRegisterUser_DuplicateUsername() throws Exception {
        String username = "existinguser";
        String password = "password123";

        when(ds.getConnection()).thenReturn(connection);
        PreparedStatement countPs = mock(PreparedStatement.class);
        ResultSet countRs = mock(ResultSet.class);
        when(connection.prepareStatement(contains("COUNT"))).thenReturn(countPs);
        when(countPs.executeQuery()).thenReturn(countRs);
        when(countRs.next()).thenReturn(true);
        when(countRs.getLong(1)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(username, password));
    }

    @Test
    public void testAuthenticateUser_Success() throws Exception {
        String username = "testuser";
        String plainPassword = "correctpassword";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("PASSWORD")).thenReturn(hashedPassword);
        when(resultSet.getLong("ID")).thenReturn(1L);
        when(resultSet.getString("USERNAME")).thenReturn(username);
        when(resultSet.getString("ROLE")).thenReturn("USER");

        User authenticated = userService.authenticateUser(username, plainPassword);

        assertNotNull(authenticated);
        assertEquals(username, authenticated.getUsername());
    }

    @Test
    public void testAuthenticateUser_Failure_WrongPassword() throws Exception {
        String username = "testuser";
        String hashedPassword = BCrypt.hashpw("correctpassword", BCrypt.gensalt());

        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("PASSWORD")).thenReturn(hashedPassword);

        User authenticated = userService.authenticateUser(username, "wrongpassword");

        assertNull(authenticated);
    }
}
