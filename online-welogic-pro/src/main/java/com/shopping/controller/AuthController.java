package com.shopping.controller;

import com.shopping.model.User;
import com.shopping.security.Secured;
import com.shopping.security.TokenUtil;
import com.shopping.security.UserPrincipal;
import com.shopping.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.Serializable;

@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private UserService userService;

    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        if (req == null || req.username == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required\"}")
                    .build();
        }
        try {
            User user = userService.registerUser(req.username, req.password);
            String token = TokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            return Response.status(Response.Status.CREATED)
                    .entity(new AuthResponse(token, user.getId(), user.getUsername(), user.getRole()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Registration failed: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        if (req == null || req.username == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required\"}")
                    .build();
        }
        try {
            User user = userService.authenticateUser(req.username, req.password);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Invalid username or password\"}")
                        .build();
            }
            String token = TokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            return Response.ok(new AuthResponse(token, user.getId(), user.getUsername(), user.getRole())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Login failed: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/me")
    @Secured
    public Response me(@Context SecurityContext securityContext) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        try {
            User user = userService.findById(principal.getId());
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"User not found\"}")
                        .build();
            }
            return Response.ok(new UserDto(user.getId(), user.getUsername(), user.getRole())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    public static class RegisterRequest {
        public String username;
        public String password;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class AuthResponse {
        public String token;
        public Long userId;
        public String username;
        public String role;

        public AuthResponse() {}
        public AuthResponse(String token, Long userId, String username, String role) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }

    public static class UserDto {
        public Long id;
        public String username;
        public String role;

        public UserDto() {}
        public UserDto(Long id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }
    }
}
