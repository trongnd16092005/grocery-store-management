package com.retail.retailstoremanagement.controller;
import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.service.AuthService;
import jakarta.servlet.ServletException;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;

@WebServlet("/login") public class LoginServlet extends HttpServlet{
 private final AuthService service=new AuthService(new JdbcUserDao());
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{if("1".equals(q.getParameter("sessionChanged")))q.setAttribute("info","Tài khoản, mật khẩu hoặc trạng thái cửa hàng vừa thay đổi. Vui lòng đăng nhập lại.");if("1".equals(q.getParameter("passwordChanged")))q.setAttribute("info","Đã đổi mật khẩu. Vui lòng đăng nhập lại bằng mật khẩu mới.");q.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(q,p);}
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{try{AppUser u=service.login(q.getParameter("storeCode"),q.getParameter("username"),q.getParameter("password"));if(u==null){q.setAttribute("error","Mã cửa hàng, tên đăng nhập hoặc mật khẩu không đúng.");q.setAttribute("storeCode",q.getParameter("storeCode"));q.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(q,p);return;}q.getSession(true);q.changeSessionId();q.getSession().setAttribute("currentUser",u);String next=(String)q.getSession().getAttribute("loginNext");q.getSession().removeAttribute("loginNext");String target=u.getRole()==com.retail.retailstoremanagement.model.UserRole.SUPER_ADMIN?"/super-admin":"/sale";p.sendRedirect(next!=null&&next.startsWith(q.getContextPath()+"/")?next:q.getContextPath()+target);}catch(Exception e){throw new ServletException(e);}}
}
