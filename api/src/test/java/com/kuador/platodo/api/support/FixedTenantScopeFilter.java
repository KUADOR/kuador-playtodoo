package com.kuador.platodo.api.support;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Test stand-in for the subdomain resolution filter (PLATODO-50): every request runs in one fixed tenant. */
public final class FixedTenantScopeFilter extends OncePerRequestFilter {

    private final TenantId tenant;

    public FixedTenantScopeFilter(TenantId tenant) {
        this.tenant = tenant;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            TenantScope.call(tenant, () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException error) {
            throw error;
        } catch (Exception unexpected) {
            throw new ServletException(unexpected);
        }
    }
}
