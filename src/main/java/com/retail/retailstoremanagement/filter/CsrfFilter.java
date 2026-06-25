package com.retail.retailstoremanagement.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfFilter implements Filter {
    private static final String PAYOS_WEBHOOK_PATH =
            "/api/payments/payos/webhook";
    private static final String SESSION_TOKEN = "csrfToken";

    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        addSecurityHeaders(httpResponse);
        if (PAYOS_WEBHOOK_PATH.equals(httpRequest.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession();
        String expectedToken = getOrCreateToken(session);
        if (isPost(httpRequest)) {
            String suppliedToken = httpRequest.getHeader("X-CSRF-Token");
            if (suppliedToken == null) {
                suppliedToken = httpRequest.getParameter(SESSION_TOKEN);
            }
            if (!constantTimeEquals(expectedToken, suppliedToken)) {
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Phiên biểu mẫu không hợp lệ. Vui lòng tải lại trang."
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
    }

    private String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_TOKEN);
        if (token != null) {
            return token;
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_TOKEN, token);
        return token;
    }

    private boolean isPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod());
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }

        int result = expected.length() ^ supplied.length();
        int comparedLength = Math.min(expected.length(), supplied.length());
        for (int index = 0; index < comparedLength; index++) {
            result |= expected.charAt(index) ^ supplied.charAt(index);
        }
        return result == 0;
    }
}
