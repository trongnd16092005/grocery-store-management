package com.retail.retailstoremanagement.util;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;

public final class RequestUtils {
    private RequestUtils() {
    }

    public static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    public static Long optionalLong(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value.isEmpty() ? null : Long.valueOf(value);
    }

    public static long requiredLong(HttpServletRequest request, String name) {
        return Long.parseLong(text(request, name));
    }

    public static int integer(HttpServletRequest request, String name, int defaultValue) {
        String value = text(request, name);
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    public static BigDecimal decimal(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    public static String consumeFlash(HttpServletRequest request, String key) {
        Object value = request.getSession().getAttribute(key);
        if (value != null) {
            request.getSession().removeAttribute(key);
        }
        return value == null ? null : value.toString();
    }

    public static void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }
}
