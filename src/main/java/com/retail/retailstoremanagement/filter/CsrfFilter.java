package com.retail.retailstoremanagement.filter;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import java.security.SecureRandom;import java.util.Base64;
public class CsrfFilter implements Filter{
 private final SecureRandom random=new SecureRandom();
 public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{HttpServletRequest q=(HttpServletRequest)request;HttpServletResponse p=(HttpServletResponse)response;HttpSession session=q.getSession();String token=(String)session.getAttribute("csrfToken");if(token==null){byte[] bytes=new byte[32];random.nextBytes(bytes);token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);session.setAttribute("csrfToken",token);}if("POST".equalsIgnoreCase(q.getMethod())){String supplied=q.getHeader("X-CSRF-Token");if(supplied==null)supplied=q.getParameter("csrfToken");if(!constantTimeEquals(token,supplied)){p.sendError(403,"Phiên biểu mẫu không hợp lệ. Vui lòng tải lại trang.");return;}}p.setHeader("X-Content-Type-Options","nosniff");p.setHeader("Referrer-Policy","same-origin");chain.doFilter(request,response);}
 private boolean constantTimeEquals(String a,String b){if(b==null)return false;int result=a.length()^b.length();for(int i=0;i<Math.min(a.length(),b.length());i++)result|=a.charAt(i)^b.charAt(i);return result==0;}
}
