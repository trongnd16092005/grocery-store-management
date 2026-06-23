package com.retail.retailstoremanagement.controller;
import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;
@WebServlet("/logout") public class LogoutServlet extends HttpServlet{protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{HttpSession s=q.getSession(false);if(s!=null)s.invalidate();p.sendRedirect(q.getContextPath()+"/login");}protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{doGet(q,p);}}
