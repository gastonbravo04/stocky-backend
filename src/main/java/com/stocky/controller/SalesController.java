package com.stocky.controller;

import com.stocky.model.Sale;
import com.stocky.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private com.stocky.repository.ProductRepository productRepository;

    @Autowired
    private com.stocky.repository.UserRepository userRepository;

    @Autowired
    private com.stocky.repository.StockMovementRepository stockMovementRepository;

    @GetMapping
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    @GetMapping("/recent")
    public List<Sale> getRecentSales() {
        return saleRepository.findTop5ByOrderByCreatedAtDesc();
    }

    static class SaleRequest {
        public java.util.List<SaleItemRequest> items;
        public String paymentMethod;
        public String payment_method; // just in case
    }

    static class SaleItemRequest {
        public java.util.Map<String, String> product;
        public int quantity;
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public Sale createSale(@RequestBody SaleRequest request) {
        Sale sale = new Sale();
        
        // Handle payment method case (camelCase vs snake_case)
        String pm = request.paymentMethod != null ? request.paymentMethod : request.payment_method;
        sale.setPaymentMethod(com.stocky.model.PaymentMethod.valueOf(pm != null ? pm.toUpperCase() : "CASH"));
        sale.setStatus(com.stocky.model.SaleStatus.COMPLETED);
        
        // Fetch a default user (since we don't have auth wired in controller yet)
        com.stocky.model.User defaultUser = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No users found in database"));
        sale.setUser(defaultUser);

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.List<com.stocky.model.SaleItem> saleItems = new java.util.ArrayList<>();

        if (request.items != null) {
            for (SaleItemRequest itemReq : request.items) {
                java.util.UUID productId = java.util.UUID.fromString(itemReq.product.get("id"));
                com.stocky.model.Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                // Validate and subtract stock
                if (product.getStockQuantity() < itemReq.quantity) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                product.setStockQuantity(product.getStockQuantity() - itemReq.quantity);
                productRepository.save(product);

                com.stocky.model.StockMovement sm = new com.stocky.model.StockMovement();
                sm.setProduct(product);
                sm.setMovementType(com.stocky.model.MovementType.SALE);
                sm.setQuantityDelta(-itemReq.quantity);
                // sm.setReferenceId(sale.getId()); // will set later when sale is saved if needed, or null
                stockMovementRepository.save(sm);

                com.stocky.model.SaleItem saleItem = new com.stocky.model.SaleItem();
                saleItem.setSale(sale);
                saleItem.setProduct(product);
                saleItem.setQuantity(itemReq.quantity);
                saleItem.setUnitPrice(product.getSalePrice());
                saleItem.setUnitCost(product.getCostPrice());

                java.math.BigDecimal subtotal = product.getSalePrice().multiply(java.math.BigDecimal.valueOf(itemReq.quantity));
                saleItem.setSubtotal(subtotal);

                total = total.add(subtotal);
                saleItems.add(saleItem);
            }
        }

        sale.setItems(saleItems);
        sale.setTotalAmount(total);

        return saleRepository.save(sale);
    }
}
