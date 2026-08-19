package com.stocky.repository;

import com.stocky.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT new map(p.name as name, SUM(si.quantity) as total_sold, SUM(si.subtotal) as revenue) " +
           "FROM SaleItem si JOIN si.product p " +
           "GROUP BY p.name ORDER BY SUM(si.quantity) DESC")
    java.util.List<java.util.Map<String, Object>> findTopSellingProducts();
}
