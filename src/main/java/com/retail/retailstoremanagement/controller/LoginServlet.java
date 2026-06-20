package com.retail.retailstoremanagement.controller;
import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.service.AuthService;
import jakarta.servlet.ServletException;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;

@WebServlet("/login") public class LoginServlet extends HttpServlet{
 private final AuthService service=new AuthService(new JdbcUserDao());
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{if(service.needsSetup()){p.sendRedirect(q.getContextPath()+"/setup");return;}}catch(Exception e){q.setAttribute("error","Không kết nối được cơ sở dữ liệu: "+e.getMessage());}q.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(q,p);}
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{AppUser u=service.login(q.getParameter("username"),q.getParameter("password"));if(u==null){q.setAttribute("error","Tên đăng nhập hoặc mật khẩu không đúng.");q.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(q,p);return;}q.getSession(true);q.changeSessionId();q.getSession().setAttribute("currentUser",u);String next=(String)q.getSession().getAttribute("loginNext");q.getSession().removeAttribute("loginNext");p.sendRedirect(next!=null&&next.startsWith(q.getContextPath()+"/")?next:q.getContextPath()+"/sale.html");}catch(Exception e){throw new ServletException(e);}}
}
