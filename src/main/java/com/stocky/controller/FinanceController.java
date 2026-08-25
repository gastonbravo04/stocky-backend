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

    @GetMapping("/expenses")
    public java.util.List<com.stocky.model.Expense> getExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    @PostMapping("/expenses")
    public com.stocky.model.Expense createExpense(@RequestBody com.stocky.model.Expense expense) {
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(java.time.LocalDate.now());
        }
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/expenses/{id}")
    public org.springframework.http.ResponseEntity<?> deleteExpense(@PathVariable java.util.UUID id) {
        if (expenseRepository.existsById(id)) {
            expenseRepository.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    @PutMapping("/expenses/{id}")
    public org.springframework.http.ResponseEntity<com.stocky.model.Expense> updateExpense(
            @PathVariable java.util.UUID id, 
            @RequestBody com.stocky.model.Expense updatedExpense) {
        
        java.util.Optional<com.stocky.model.Expense> existingExpenseOpt = expenseRepository.findById(id);
        if (existingExpenseOpt.isPresent()) {
            com.stocky.model.Expense existingExpense = existingExpenseOpt.get();
            existingExpense.setConcept(updatedExpense.getConcept());
            existingExpense.setAmount(updatedExpense.getAmount());
            existingExpense.setCategory(updatedExpense.getCategory());
            if (updatedExpense.getExpenseDate() != null) {
                existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
            }
            existingExpense.setNotes(updatedExpense.getNotes());
            
            com.stocky.model.Expense saved = expenseRepository.save(existingExpense);
            return org.springframework.http.ResponseEntity.ok(saved);
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }
}
