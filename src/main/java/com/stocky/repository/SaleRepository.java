package com.stocky.repository;

import com.stocky.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    java.util.List<Sale> findTop5ByOrderByCreatedAtDesc();
    java.util.List<Sale> findByUserOrderByCreatedAtDesc(com.stocky.model.User user);
    java.util.List<Sale> findTop5ByUserOrderByCreatedAtDesc(com.stocky.model.User user);
    java.util.Optional<Sale> findByIdAndUser(UUID id, com.stocky.model.User user);
    
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    java.math.BigDecimal sumTotalSales();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.user = :user")
    java.math.BigDecimal sumTotalSalesByUser(@org.springframework.data.repository.query.Param("user") com.stocky.model.User user);
}
