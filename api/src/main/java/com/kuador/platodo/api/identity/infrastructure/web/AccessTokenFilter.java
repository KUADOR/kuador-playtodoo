package com.kuador.platodo.api.identity.infrastructure.web;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.kuador.platodo.api.identity.application.AuthenticateRequest;
import com.kuador.platodo.api.identity.application.RequestAuthentication;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs right after tenant resolution, so it sees the request inside the {@code TenantScope} of its complex (or
 * outside any scope on the global host). A request without {@code Authorization} continues anonymously and each
 * operation decides whether it needs an account. A bearer token is checked with the acceptance rules of
 * {@link AuthenticateRequest}: an accepted one runs the rest of the request as its {@link CurrentPrincipal}; any
 * other value is rejected before a handler runs.
 */
final class AccessTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticateRequest authenticateRequest;
    private final HandlerExceptionResolver problemRenderer;

    AccessTokenFilter(AuthenticateRequest authenticateRequest, HandlerExceptionResolver problemRenderer) {
        this.authenticateRequest = authenticateRequest;
        this.problemRenderer = problemRenderer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!hasBearerToken(authorization)) {
            reject(IdentityProblem.UNAUTHENTICATED, request, response);
            return;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).strip();
        switch (authenticateRequest.authenticate(token)) {
            case RequestAuthentication.Authenticated(AuthenticatedPrincipal principal) ->
                    continueAs(principal, request, response, chain);
            case RequestAuthentication.Rejected(RequestAuthentication.Reason reason) ->
                    reject(IdentityProblem.valueOf(reason.name()), request, response);
        }
    }

    private static boolean hasBearerToken(String authorization) {
        return authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && !authorization.substring(BEARER_PREFIX.length()).isBlank();
    }

    private static void continueAs(AuthenticatedPrincipal principal, HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            CurrentPrincipal.<Void, Exception>call(principal, () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException failure) {
            throw failure;
        } catch (Exception checkedFailure) {
            throw new ServletException(checkedFailure);
        }
    }

    private void reject(IdentityProblem problem, HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        AccessTokenRejectedException rejection = new AccessTokenRejectedException(problem);
        if (problemRenderer.resolveException(request, response, null, rejection) == null) {
            throw new ServletException("No handler renders access token problems", rejection);
        }
    }
}
