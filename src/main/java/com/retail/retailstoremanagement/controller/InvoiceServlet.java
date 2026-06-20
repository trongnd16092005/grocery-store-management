package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.service.InvoiceService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/invoices")
public class InvoiceServlet extends HttpServlet {
    private final InvoiceService service=new InvoiceService(new JdbcInvoiceDao());
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try{String q=RequestUtils.text(req,"q"),status=RequestUtils.text(req,"status");int page=Math.max(1,RequestUtils.integer(req,"page",1));req.setAttribute("invoices",service.findAll(q,status,20,page));req.setAttribute("keyword",q);req.setAttribute("selectedStatus",status);req.setAttribute("page",page);String id=RequestUtils.text(req,"id");if(!id.isEmpty())req.setAttribute("selectedInvoice",service.findById(Long.parseLong(id)));req.setAttribute("flashSuccess",RequestUtils.consumeFlash(req,"flashSuccess"));req.setAttribute("flashError",RequestUtils.consumeFlash(req,"flashError"));req.getRequestDispatcher("/WEB-INF/views/invoices.jsp").forward(req,resp);}catch(Exception e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        try{AppUser user=(AppUser)req.getSession().getAttribute("currentUser");if(user==null||user.getRole()!=UserRole.ADMIN){resp.sendError(403,"Chỉ quản trị viên được hủy hóa đơn.");return;}service.cancel(Long.parseLong(req.getParameter("id")),user.getId());RequestUtils.flash(req,"flashSuccess","Đã hủy hóa đơn và hoàn lại tồn kho.");}catch(Exception e){RequestUtils.flash(req,"flashError",e.getMessage());}resp.sendRedirect(req.getContextPath()+"/invoices");
    }
}
