package com.stocky.controller;

import com.stocky.model.Product;
import com.stocky.model.User;
import com.stocky.repository.ProductRepository;
import com.stocky.service.SecurityHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.stocky.repository.StockMovementRepository stockMovementRepository;

    @Autowired
    private SecurityHelperService securityHelper;

    @Autowired
    private com.stocky.repository.UserRepository userRepository;

    @GetMapping
    public List<Product> getAllProducts() {
        User user = securityHelper.getRequiredCurrentUser();
        List<Product> userProducts = productRepository.findByUser(user);

        // Si es el usuario principal/admin y hay productos previos sin dueño, asignárselos
        if (userProducts.isEmpty() && (user.getRole() == com.stocky.model.UserRole.ADMIN || userRepository.count() <= 1)) {
            List<Product> legacyProducts = productRepository.findAll().stream()
                    .filter(p -> p.getUser() == null)
                    .toList();
            if (!legacyProducts.isEmpty()) {
                legacyProducts.forEach(p -> p.setUser(user));
                productRepository.saveAll(legacyProducts);
                return legacyProducts;
            }
        }
        return userProducts;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        User user = securityHelper.getRequiredCurrentUser();
        Optional<Product> product = productRepository.findByIdAndUser(id, user);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        User user = securityHelper.getRequiredCurrentUser();
        product.setUser(user);

        Product saved = productRepository.save(product);
        if (saved.getStockQuantity() != 0) {
            com.stocky.model.StockMovement sm = new com.stocky.model.StockMovement();
            sm.setProduct(saved);
            sm.setMovementType(com.stocky.model.MovementType.PURCHASE);
            sm.setQuantityDelta(saved.getStockQuantity());
            stockMovementRepository.save(sm);
        }
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product productDetails) {
        User user = securityHelper.getRequiredCurrentUser();
        Optional<Product> productOptional = productRepository.findByIdAndUser(id, user);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            int oldStock = product.getStockQuantity();
            int newStock = productDetails.getStockQuantity();
            
            product.setName(productDetails.getName());
            product.setSku(productDetails.getSku());
            product.setSalePrice(productDetails.getSalePrice());
            product.setCostPrice(productDetails.getCostPrice());
            product.setStockQuantity(newStock);
            
            Product saved = productRepository.save(product);
            
            if (oldStock != newStock) {
                com.stocky.model.StockMovement sm = new com.stocky.model.StockMovement();
                sm.setProduct(saved);
                sm.setMovementType(com.stocky.model.MovementType.ADJUSTMENT);
                sm.setQuantityDelta(newStock - oldStock);
                stockMovementRepository.save(sm);
            }
            
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        User user = securityHelper.getRequiredCurrentUser();
        Optional<Product> productOptional = productRepository.findByIdAndUser(id, user);
        if (productOptional.isPresent()) {
            productRepository.delete(productOptional.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
