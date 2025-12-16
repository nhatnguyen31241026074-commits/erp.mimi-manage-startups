package com.techforge.erp.controller;

import com.techforge.erp.model.Invoice;
import com.techforge.erp.service.MomoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final MomoService momoService;

    public PaymentController(MomoService momoService) {
        this.momoService = momoService;
    }

    @PostMapping("/pay-invoice/{invoiceId}")
    public ResponseEntity<?> payInvoice(@PathVariable String invoiceId) {
        // Mock fetching the Invoice (no DB available yet)
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setAmount(50000.0);
        invoice.setStatus("PENDING");

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            return ResponseEntity.badRequest().body("Invoice already paid");
        }

        Map<String, Object> result = momoService.createPaymentUrl(invoice);
        return ResponseEntity.ok(result);
    }
}
