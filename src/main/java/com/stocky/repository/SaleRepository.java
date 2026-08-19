package com.stocky.repository;

import com.stocky.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    java.util.List<Sale> findTop5ByOrderByCreatedAtDesc();
    
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    java.math.BigDecimal sumTotalSales();
}
