package com.stocky.controller;

import com.stocky.model.Sale;
import com.stocky.model.User;
import com.stocky.repository.SaleRepository;
import com.stocky.service.SecurityHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private com.stocky.repository.ProductRepository productRepository;

    @Autowired
    private com.stocky.repository.StockMovementRepository stockMovementRepository;

    @Autowired
    private SecurityHelperService securityHelper;

    @GetMapping
    public List<Sale> getAllSales() {
        User user = securityHelper.getRequiredCurrentUser();
        return saleRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @GetMapping("/recent")
    public List<Sale> getRecentSales() {
        User user = securityHelper.getRequiredCurrentUser();
        return saleRepository.findTop5ByUserOrderByCreatedAtDesc(user);
    }

    @GetMapping("/{id}")
    public Sale getSaleById(@PathVariable UUID id) {
        User user = securityHelper.getRequiredCurrentUser();
        return saleRepository.findByIdAndUser(id, user).orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    public static class SaleRequest {
        public java.util.List<SaleItemRequest> items;
        public String paymentMethod;
        public String payment_method;
    }

    public static class SaleItemRequest {
        public java.util.Map<String, String> product;
        public int quantity;
    }

    // ========================================================
    // REGISTRAR VENTA Y DESCONTAR STOCK (MÉTODO PRINCIPAL)
    // ========================================================
    @PostMapping
    @org.springframework.transaction.annotation.Transactional // Garantiza que si falla algún ítem, hace Rollback completo
    public Sale createSale(@RequestBody SaleRequest request) {
        // 1. Obtener el usuario autenticado a partir del token JWT (Aislamiento de datos)
        User currentUser = securityHelper.getRequiredCurrentUser();

        Sale sale = new Sale();
        String pm = request.paymentMethod != null ? request.paymentMethod : request.payment_method;
        sale.setPaymentMethod(com.stocky.model.PaymentMethod.valueOf(pm != null ? pm.toUpperCase() : "CASH"));
        sale.setStatus(com.stocky.model.SaleStatus.COMPLETED);
        sale.setUser(currentUser);

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.List<com.stocky.model.SaleItem> saleItems = new java.util.ArrayList<>();

        if (request.items != null) {
            for (SaleItemRequest itemReq : request.items) {
                UUID productId = UUID.fromString(itemReq.product.get("id"));
                com.stocky.model.Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                // 2. VALIDAR DISPONIBILIDAD DE STOCK EN EL SERVIDOR
                if (product.getStockQuantity() < itemReq.quantity) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }

                // 3. DESCONTAR EL STOCK DEL PRODUCTO Y PERSISTIR EN POSTGRESQL
                product.setStockQuantity(product.getStockQuantity() - itemReq.quantity);
                productRepository.save(product);

                // 4. Registrar movimiento de stock para auditoría histórica
                com.stocky.model.StockMovement sm = new com.stocky.model.StockMovement();
                sm.setProduct(product);
                sm.setMovementType(com.stocky.model.MovementType.SALE);
                sm.setQuantityDelta(-itemReq.quantity);
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
