package com.shopping.controller;

import com.shopping.model.CartItem;
import com.shopping.security.Secured;
import com.shopping.security.UserPrincipal;
import com.shopping.service.CartService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Path("/cart")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private CartService cartService;

    @GET
    @Secured
    public Response getCart(@Context SecurityContext securityContext) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        try {
            List<CartItem> items = cartService.getCartForUser(principal.getId());
            CartDto cartDto = new CartDto();
            cartDto.items = new ArrayList<>();
            double total = 0.0;
            for (CartItem item : items) {
                CartItemDto itemDto = new CartItemDto();
                itemDto.id = item.getId();
                itemDto.productId = item.getProduct().getId();
                itemDto.productName = item.getProduct().getName();
                itemDto.productPrice = item.getProduct().getPrice();
                itemDto.productImageUrl = item.getProduct().getImageUrl();
                itemDto.quantity = item.getQuantity();
                itemDto.inventory = item.getProduct().getInventory();
                cartDto.items.add(itemDto);
                total += item.getProduct().getPrice() * item.getQuantity();
            }
            cartDto.totalAmount = Math.round(total * 100.0) / 100.0;
            return Response.ok(cartDto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Secured
    public Response addToCart(@Context SecurityContext securityContext, CartRequest req) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        if (req == null || req.productId == null || req.quantity == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Product ID and quantity are required\"}")
                    .build();
        }
        try {
            CartItem item = cartService.addToCart(principal.getId(), req.productId, req.quantity);
            return Response.ok(item).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Secured
    public Response updateQuantity(@Context SecurityContext securityContext, CartRequest req) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        if (req == null || req.productId == null || req.quantity == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Product ID and quantity are required\"}")
                    .build();
        }
        try {
            CartItem item = cartService.updateQuantity(principal.getId(), req.productId, req.quantity);
            return Response.ok(item != null ? item : "{\"message\":\"Item removed from cart\"}").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{productId}")
    @Secured
    public Response removeFromCart(@Context SecurityContext securityContext, @PathParam("productId") Long productId) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        try {
            cartService.removeFromCart(principal.getId(), productId);
            return Response.ok("{\"message\":\"Item removed from cart successfully\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/clear")
    @Secured
    public Response clearCart(@Context SecurityContext securityContext) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        try {
            cartService.clearCart(principal.getId());
            return Response.ok("{\"message\":\"Cart cleared successfully\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/checkout")
    @Secured
    public Response checkout(@Context SecurityContext securityContext) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        try {
            cartService.checkout(principal.getId());
            return Response.ok("{\"message\":\"Checkout successful\"}").build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    public static class CartRequest {
        public Long productId;
        public Integer quantity;
    }

    public static class CartItemDto {
        public Long id;
        public Long productId;
        public String productName;
        public Double productPrice;
        public String productImageUrl;
        public Integer quantity;
        public Integer inventory;
    }

    public static class CartDto {
        public List<CartItemDto> items;
        public Double totalAmount;
    }
}
