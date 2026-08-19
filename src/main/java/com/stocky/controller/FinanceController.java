package com.stocky.controller;

import com.stocky.repository.ExpenseRepository;
import com.stocky.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/cash-flow")
    public Map<String, BigDecimal> getCashflow() {
        BigDecimal totalIncome = saleRepository.sumTotalSales();
        BigDecimal totalExpenses = expenseRepository.sumTotalExpenses();
        BigDecimal balance = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> cashflow = new HashMap<>();
        cashflow.put("income", totalIncome);
        cashflow.put("expenses", totalExpenses);
        cashflow.put("net_profit", balance);

        return cashflow;
    }
}
