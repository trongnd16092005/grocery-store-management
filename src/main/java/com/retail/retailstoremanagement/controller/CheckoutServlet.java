package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.service.InvoiceService;
import com.retail.retailstoremanagement.util.JsonUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/checkout")
public class CheckoutServlet extends HttpServlet {
    private final InvoiceService service = new InvoiceService(new JdbcInvoiceDao());
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        req.setCharacterEncoding("UTF-8"); resp.setContentType("application/json;charset=UTF-8");
        try {
            String[] codes=req.getParameterValues("productCode"); String[] quantities=req.getParameterValues("quantity");
            if(codes==null||quantities==null||codes.length!=quantities.length)throw new IllegalArgumentException("Giỏ hàng không hợp lệ.");
            Map<String,Integer> items=new LinkedHashMap<>();
            for(int i=0;i<codes.length;i++)items.merge(codes[i],Integer.parseInt(quantities[i]),Integer::sum);
            String cashText=req.getParameter("cashReceived"); BigDecimal cash=cashText==null||cashText.isBlank()?null:new BigDecimal(cashText);
            String discountValueText=req.getParameter("discountValue");
            BigDecimal discountValue=discountValueText==null||discountValueText.isBlank()?null:new BigDecimal(discountValueText);
            AppUser user=(AppUser)req.getSession().getAttribute("currentUser");
            Invoice invoice=service.checkout(items,req.getParameter("customerCode"),req.getParameter("paymentMethod"),cash,
                    req.getParameter("discountType"),discountValue,user==null?null:user.getId());
            resp.getWriter().printf("{\"success\":true,\"invoiceId\":%d,\"code\":\"%s\",\"subtotal\":%s,\"discount\":%s,\"total\":%s,\"change\":%s}",
                    invoice.getId(),JsonUtils.escape(invoice.getCode()),invoice.getSubtotal(),invoice.getDiscountAmount(),
                    invoice.getTotalAmount(),invoice.getChangeAmount()==null?"null":invoice.getChangeAmount());
        } catch(Exception e){resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);resp.getWriter().print("{\"success\":false,\"message\":\""+JsonUtils.escape(e.getMessage())+"\"}");}
    }
}
