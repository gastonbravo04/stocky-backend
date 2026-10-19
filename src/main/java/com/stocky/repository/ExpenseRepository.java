package com.stocky.repository;

import com.stocky.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    java.math.BigDecimal sumTotalExpenses();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    java.math.BigDecimal sumTotalExpensesByUser(@org.springframework.data.repository.query.Param("user") com.stocky.model.User user);
    
    java.util.List<Expense> findAllByOrderByExpenseDateDesc();
    java.util.List<Expense> findByUserOrderByExpenseDateDesc(com.stocky.model.User user);
    java.util.Optional<Expense> findByIdAndUser(UUID id, com.stocky.model.User user);
}
