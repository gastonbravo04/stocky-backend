package com.stocky.controller;

import com.stocky.model.Product;
import com.stocky.repository.ProductRepository;
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

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
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
        Optional<Product> productOptional = productRepository.findById(id);
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
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
