package com.techforge.erp.controller;

import com.techforge.erp.model.Invoice;
import com.techforge.erp.service.MomoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Payment", description = "Payment integration endpoints (MoMo)")
public class PaymentController {

    private final MomoService momoService;

    public PaymentController(MomoService momoService) {
        this.momoService = momoService;
    }

    @PostMapping("/pay-invoice/{invoiceId}")
    @Operation(summary = "Initiate MoMo payment for an invoice")
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
