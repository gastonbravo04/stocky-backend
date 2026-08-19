package com.stocky.controller;

import com.stocky.repository.SaleItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private SaleItemRepository saleItemRepository;

    @GetMapping("/top-products")
    public List<Map<String, Object>> getTopProducts() {
        // Obtenemos los productos más vendidos y limitamos a 5 para el dashboard
        return saleItemRepository.findTopSellingProducts()
                .stream()
                .limit(5)
                .collect(Collectors.toList());
    }
}
