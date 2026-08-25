package com.stocky.controller;

import com.stocky.repository.SaleItemRepository;
import com.stocky.repository.SaleRepository;
import com.stocky.repository.ExpenseRepository;
import com.stocky.model.Sale;
import com.stocky.model.Expense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private SaleItemRepository saleItemRepository;
    
    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/top-products")
    public List<Map<String, Object>> getTopProducts() {
        return saleItemRepository.findTopSellingProducts()
                .stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Sale> sales = saleRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();

        // Agrupar ventas por mes (YYYY-MM)
        Map<String, BigDecimal> salesByMonth = new HashMap<>();
        for (Sale s : sales) {
            String month = s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            salesByMonth.put(month, salesByMonth.getOrDefault(month, BigDecimal.ZERO).add(s.getTotalAmount()));
        }

        // Agrupar gastos por mes (YYYY-MM)
        Map<String, BigDecimal> expensesByMonth = new HashMap<>();
        for (Expense e : expenses) {
            String month = e.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            expensesByMonth.put(month, expensesByMonth.getOrDefault(month, BigDecimal.ZERO).add(e.getAmount()));
        }

        // Agrupar ventas por año (YYYY)
        Map<String, BigDecimal> salesByYear = new HashMap<>();
        for (Sale s : sales) {
            String year = String.valueOf(s.getCreatedAt().getYear());
            salesByYear.put(year, salesByYear.getOrDefault(year, BigDecimal.ZERO).add(s.getTotalAmount()));
        }

        // Agrupar gastos por año (YYYY)
        Map<String, BigDecimal> expensesByYear = new HashMap<>();
        for (Expense e : expenses) {
            String year = String.valueOf(e.getExpenseDate().getYear());
            expensesByYear.put(year, expensesByYear.getOrDefault(year, BigDecimal.ZERO).add(e.getAmount()));
        }

        // Obtener todos los meses únicos
        Set<String> allMonths = new TreeSet<>();
        allMonths.addAll(salesByMonth.keySet());
        allMonths.addAll(expensesByMonth.keySet());

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        for (String m : allMonths) {
            Map<String, Object> data = new HashMap<>();
            data.put("period", m);
            data.put("income", salesByMonth.getOrDefault(m, BigDecimal.ZERO));
            data.put("expenses", expensesByMonth.getOrDefault(m, BigDecimal.ZERO));
            monthlyData.add(data);
        }

        // Obtener todos los años únicos
        Set<String> allYears = new TreeSet<>();
        allYears.addAll(salesByYear.keySet());
        allYears.addAll(expensesByYear.keySet());

        List<Map<String, Object>> yearlyData = new ArrayList<>();
        for (String y : allYears) {
            Map<String, Object> data = new HashMap<>();
            data.put("period", y);
            data.put("income", salesByYear.getOrDefault(y, BigDecimal.ZERO));
            data.put("expenses", expensesByYear.getOrDefault(y, BigDecimal.ZERO));
            yearlyData.add(data);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("monthly", monthlyData);
        response.put("yearly", yearlyData);

        return response;
    }

    @GetMapping("/movements")
    public List<Map<String, Object>> getMovements() {
        List<Sale> sales = saleRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();

        List<Map<String, Object>> movements = new ArrayList<>();

        for (Sale s : sales) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId().toString());
            m.put("type", "INCOME");
            m.put("amount", s.getTotalAmount());
            m.put("date", s.getCreatedAt().toString());
            m.put("description", "Venta #" + s.getId().toString().substring(0, 6));
            movements.add(m);
        }

        for (Expense e : expenses) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId().toString());
            m.put("type", "EXPENSE");
            m.put("amount", e.getAmount());
            m.put("date", e.getCreatedAt().toString()); // Assuming createdAt exists, otherwise expenseDate + T00:00:00
            m.put("description", e.getConcept());
            movements.add(m);
        }

        // Sort descending by date
        movements.sort((a, b) -> {
            String dateA = (String) a.get("date");
            String dateB = (String) b.get("date");
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        return movements;
    }
}
