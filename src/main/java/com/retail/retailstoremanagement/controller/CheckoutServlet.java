package com.retail.retailstoremanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.service.InvoiceService;
import com.retail.retailstoremanagement.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/checkout")
public class CheckoutServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final InvoiceService service = new InvoiceService(new JdbcInvoiceDao());
    private final PaymentService paymentService = new PaymentService();
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        req.setCharacterEncoding("UTF-8"); resp.setContentType("application/json;charset=UTF-8");
        try {
            String[] codes=req.getParameterValues("productCode"); String[] quantities=req.getParameterValues("quantity");
            if(codes==null||quantities==null||codes.length!=quantities.length)throw new IllegalArgumentException("Giỏ hàng không hợp lệ.");
            Map<String,Integer> items=new LinkedHashMap<>();
            for(int i=0;i<codes.length;i++)items.merge(codes[i],Integer.parseInt(quantities[i]),Integer::sum);
            String cashText=req.getParameter("cashReceived"); BigDecimal cash=cashText==null||cashText.isBlank()?null:new BigDecimal(cashText);
            int pointsToRedeem = parsePoints(req.getParameter("pointsToRedeem"));
            AppUser user=(AppUser)req.getSession().getAttribute("currentUser");
            if ("QR".equalsIgnoreCase(req.getParameter("paymentMethod"))) {
                PaymentTransaction payment = paymentService.startQr(
                        items, req.getParameter("customerCode"),
                        req.getParameter("discountCode"), pointsToRedeem, user);
                Map<String,Object> result=new LinkedHashMap<>();
                result.put("success",true);result.put("pending",true);
                result.put("invoiceId",payment.getInvoiceId());
                result.put("code",payment.getInvoiceCode());
                result.put("orderCode",payment.getOrderCode());
                result.put("total",payment.getAmount());
                result.put("invoiceTotal", payment.getInvoiceTotalAmount());
                result.put("qrCode",payment.getQrCode());
                result.put("checkoutUrl",payment.getCheckoutUrl());
                result.put("expiresAt",payment.getExpiresAt().toString());
                JSON.writeValue(resp.getWriter(),result);
                return;
            }
            Invoice invoice=service.checkout(items,req.getParameter("customerCode"),req.getParameter("paymentMethod"),cash,
                    req.getParameter("discountCode"),pointsToRedeem,user);
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("success",true);result.put("pending",false);
            result.put("invoiceId",invoice.getId());result.put("code",invoice.getCode());
            result.put("subtotal",invoice.getSubtotal());result.put("discount",invoice.getDiscountAmount());
            result.put("pointsRedeemed", invoice.getPointsRedeemed());
            result.put("pointsDiscount", invoice.getPointsDiscountAmount());
            result.put("pointsEarned", invoice.getPointsEarned());
            result.put("total",invoice.getTotalAmount());result.put("change",invoice.getChangeAmount());
            JSON.writeValue(resp.getWriter(),result);
        } catch(Exception e){resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);JSON.writeValue(resp.getWriter(),Map.of("success",false,"message",e.getMessage()==null?"Thanh toán thất bại.":e.getMessage()));}
    }

    private int parsePoints(String value) {
        if (value == null || value.isBlank()) return 0;
        return Integer.parseInt(value);
    }
}
