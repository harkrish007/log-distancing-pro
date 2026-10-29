package com.shopping.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class SecuredFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            abortWithUnauthorized(requestContext);
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        TokenUtil.TokenData tokenData = TokenUtil.verifyToken(token);

        if (tokenData == null) {
            abortWithUnauthorized(requestContext);
            return;
        }

        final UserPrincipal userPrincipal = new UserPrincipal(
            tokenData.getUserId(),
            tokenData.getUsername(),
            tokenData.getRole()
        );

        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return userPrincipal;
            }

            @Override
            public boolean isUserInRole(String role) {
                return userPrincipal.getRole().equalsIgnoreCase(role);
            }

            @Override
            public boolean isSecure() {
                return requestContext.getSecurityContext().isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        });
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"shopping\"")
                    .entity("{\"error\":\"Unauthorized or expired token\"}")
                    .type("application/json")
                    .build()
        );
    }
}
