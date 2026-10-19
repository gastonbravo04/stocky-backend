package com.stocky.repository;

import com.stocky.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(String sku);
    java.util.List<Product> findByUser(com.stocky.model.User user);
    Optional<Product> findByIdAndUser(UUID id, com.stocky.model.User user);
    Optional<Product> findBySkuAndUser(String sku, com.stocky.model.User user);
}
