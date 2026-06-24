package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.service.InvoiceService;
import com.retail.retailstoremanagement.service.StoreService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/invoices/print")
public class PrintInvoiceServlet extends HttpServlet {
    private final InvoiceService invoiceService = new InvoiceService(new JdbcInvoiceDao());
    private final StoreService storeService = new StoreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Invoice invoice = invoiceService.findById(RequestUtils.requiredLong(request, "id"));
            if (invoice == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hóa đơn.");
                return;
            }
            request.setAttribute("invoice", invoice);
            request.setAttribute("store", storeService.findCurrent());
            request.getRequestDispatcher("/WEB-INF/views/invoice-print.jsp")
                    .forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã hóa đơn không hợp lệ.");
        } catch (Exception exception) {
            throw new ServletException("Không thể tải hóa đơn để in.", exception);
        }
    }
}
