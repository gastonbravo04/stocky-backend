package com.stocky.controller;

import com.stocky.model.Expense;
import com.stocky.model.User;
import com.stocky.repository.ExpenseRepository;
import com.stocky.repository.SaleRepository;
import com.stocky.service.SecurityHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SecurityHelperService securityHelper;

    @GetMapping("/cash-flow")
    public Map<String, BigDecimal> getCashflow() {
        User user = securityHelper.getRequiredCurrentUser();

        BigDecimal totalIncome = saleRepository.sumTotalSalesByUser(user);
        BigDecimal totalExpenses = expenseRepository.sumTotalExpensesByUser(user);
        BigDecimal balance = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> cashflow = new HashMap<>();
        cashflow.put("income", totalIncome);
        cashflow.put("expenses", totalExpenses);
        cashflow.put("net_profit", balance);

        return cashflow;
    }

    @GetMapping("/expenses")
    public List<Expense> getExpenses() {
        User user = securityHelper.getRequiredCurrentUser();
        return expenseRepository.findByUserOrderByExpenseDateDesc(user);
    }

    @PostMapping("/expenses")
    public Expense createExpense(@RequestBody Expense expense) {
        User user = securityHelper.getRequiredCurrentUser();
        expense.setUser(user);

        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable UUID id) {
        User user = securityHelper.getRequiredCurrentUser();
        Optional<Expense> existing = expenseRepository.findByIdAndUser(id, user);
        if (existing.isPresent()) {
            expenseRepository.delete(existing.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable UUID id, 
            @RequestBody Expense updatedExpense) {
        
        User user = securityHelper.getRequiredCurrentUser();
        Optional<Expense> existingExpenseOpt = expenseRepository.findByIdAndUser(id, user);
        if (existingExpenseOpt.isPresent()) {
            Expense existingExpense = existingExpenseOpt.get();
            existingExpense.setConcept(updatedExpense.getConcept());
            existingExpense.setAmount(updatedExpense.getAmount());
            existingExpense.setCategory(updatedExpense.getCategory());
            if (updatedExpense.getExpenseDate() != null) {
                existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
            }
            existingExpense.setNotes(updatedExpense.getNotes());
            
            Expense saved = expenseRepository.save(existingExpense);
            return ResponseEntity.ok(saved);
        }
        return ResponseEntity.notFound().build();
    }
}
