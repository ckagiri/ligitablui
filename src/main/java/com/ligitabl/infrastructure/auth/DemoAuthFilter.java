package com.ligitabl.infrastructure.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;

/**
 * Demo authentication filter that provides a Principal from session.
 *
 * <p>This is NOT real security - it's for demo/testing purposes only.
 * In production, use Spring Security with proper authentication.</p>
 *
 * <p>How it works:</p>
 * <ul>
 *   <li>On login/register, AuthController stores user ID in session as "DEMO_USER_ID"</li>
 *   <li>This filter wraps the request to provide a Principal with that user ID</li>
 *   <li>Controllers can then use Principal to identify the logged-in user</li>
 * </ul>
 */
@Component
public class DemoAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_USER_ID_KEY = "DEMO_USER_ID";
    public static final String SESSION_DISPLAY_NAME_KEY = "DEMO_DISPLAY_NAME";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String userId = null;

        if (session != null) {
            userId = (String) session.getAttribute(SESSION_USER_ID_KEY);
        }

        if (userId != null) {
            // Wrap request to provide Principal
            final String finalUserId = userId;
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public Principal getUserPrincipal() {
                    return () -> finalUserId;
                }

                @Override
                public String getRemoteUser() {
                    return finalUserId;
                }
            };
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
