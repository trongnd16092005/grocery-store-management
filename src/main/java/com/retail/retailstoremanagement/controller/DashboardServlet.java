package com.retail.retailstoremanagement.controller;
import com.retail.retailstoremanagement.dao.impl.JdbcDashboardDao;import jakarta.servlet.*;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;
@WebServlet("/dashboard") public class DashboardServlet extends HttpServlet{
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{q.setAttribute("stats",new JdbcDashboardDao().load());q.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(q,p);}catch(Exception e){throw new ServletException(e);}}
}
