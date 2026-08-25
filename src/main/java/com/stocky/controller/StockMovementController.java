package com.stocky.controller;

import com.stocky.model.StockMovement;
import com.stocky.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/movements")
@CrossOrigin(origins = "*")
public class StockMovementController {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @GetMapping
    public List<StockMovement> getStockMovements() {
        List<StockMovement> movements = stockMovementRepository.findAll();
        // Sort descending by date
        movements.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return movements;
    }
}
