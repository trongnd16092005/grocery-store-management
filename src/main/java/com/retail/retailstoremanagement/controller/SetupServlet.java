package com.retail.retailstoremanagement.controller;
import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;import com.retail.retailstoremanagement.model.AppUser;import com.retail.retailstoremanagement.service.AuthService;import jakarta.servlet.ServletException;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;
@WebServlet("/setup") public class SetupServlet extends HttpServlet{
 private final AuthService service=new AuthService(new JdbcUserDao());
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{if(!service.needsSetup()){p.sendRedirect(q.getContextPath()+"/login");return;}}catch(Exception e){q.setAttribute("error",e.getMessage());}q.getRequestDispatcher("/WEB-INF/views/setup.jsp").forward(q,p);}
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{if(!service.needsSetup()){p.sendError(409);return;}AppUser u=service.setupAdmin(q.getParameter("username"),q.getParameter("password"),q.getParameter("fullName"));q.getSession(true).setAttribute("currentUser",u);p.sendRedirect(q.getContextPath()+"/sale.html");}catch(Exception e){q.setAttribute("error",e.getMessage());q.getRequestDispatcher("/WEB-INF/views/setup.jsp").forward(q,p);}}
}
