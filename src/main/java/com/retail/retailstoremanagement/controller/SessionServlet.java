package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.util.JsonUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/session")
public class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AppUser user = (AppUser) request.getSession()
                .getAttribute("currentUser");
        String csrfToken = (String) request.getSession()
                .getAttribute("csrfToken");

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().printf(
                "{\"username\":\"%s\",\"role\":\"%s\",\"csrfToken\":\"%s\"}",
                JsonUtils.escape(user.getUsername()),
                user.getRole(),
                JsonUtils.escape(csrfToken)
        );
    }
}
