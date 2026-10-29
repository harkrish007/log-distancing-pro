package com.shopping.controller;

import com.shopping.model.Product;
import com.shopping.security.Secured;
import com.shopping.security.UserPrincipal;
import com.shopping.service.ProductService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.Serializable;
import java.util.List;

@Path("/products")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private ProductService productService;

    @GET
    public Response getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return Response.ok(products).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") Long id) {
        try {
            Product p = productService.findById(id);
            if (p == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Product not found\"}")
                        .build();
            }
            return Response.ok(p).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Secured
    public Response addProduct(@Context SecurityContext securityContext, Product product) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Only admin can add products\"}")
                    .build();
        }
        if (product == null || product.getName() == null || product.getPrice() == null || product.getInventory() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Product name, price, and inventory are required\"}")
                    .build();
        }
        try {
            Product saved = productService.saveProduct(product);
            return Response.status(Response.Status.CREATED).entity(saved).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
