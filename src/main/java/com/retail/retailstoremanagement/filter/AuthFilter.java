package com.retail.retailstoremanagement.filter;
import com.retail.retailstoremanagement.model.*;import jakarta.servlet.*;import jakarta.servlet.annotation.WebFilter;import jakarta.servlet.http.*;import java.io.IOException;import java.util.Set;
@WebFilter("/*") public class AuthFilter implements Filter{
 private static final Set<String> PUBLIC=Set.of("/login","/setup");
 private static final Set<String> ADMIN=Set.of("/products","/products.html","/categories","/categories.html","/inventory","/inventory.html","/users");
 public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{HttpServletRequest q=(HttpServletRequest)request;HttpServletResponse p=(HttpServletResponse)response;String path=q.getRequestURI().substring(q.getContextPath().length());if(PUBLIC.contains(path)||path.startsWith("/assets/")||path.equals("/favicon.ico")){chain.doFilter(request,response);return;}AppUser u=(AppUser)q.getSession().getAttribute("currentUser");if(u==null){if(path.startsWith("/api/")){p.sendError(401,"Vui lòng đăng nhập.");return;}q.getSession().setAttribute("loginNext",q.getRequestURI());p.sendRedirect(q.getContextPath()+"/login");return;}if(ADMIN.contains(path)&&u.getRole()!=UserRole.ADMIN){p.sendError(403,"Chức năng này chỉ dành cho quản trị viên.");return;}chain.doFilter(request,response);}
}
