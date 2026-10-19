package com.stocky.controller;

import com.stocky.model.Product;
import com.stocky.model.Sale;
import com.stocky.model.User;
import com.stocky.service.SecurityHelperService;
import com.stocky.repository.ProductRepository;
import com.stocky.repository.SaleRepository;
import com.stocky.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SecurityHelperService securityHelper;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Map.of("reply", "Por favor, escribe un mensaje.");
        }

        User user = securityHelper.getRequiredCurrentUser();

        // 1. Gather Context
        String systemPrompt = buildSystemPrompt(user);

        // 2. Call Gemini API
        String aiResponse = aiService.generateResponse(systemPrompt, userMessage);

        return Map.of("reply", aiResponse);
    }

    private String buildSystemPrompt(User user) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres 'Stocky AI', el asistente inteligente integrado en el sistema de Punto de Venta (POS) llamado Stocky. ");
        prompt.append("Tu rol es ayudar al dueño del negocio (" + user.getFullName() + ") a entender el software y brindarle reportes automatizados basados en los datos en tiempo real de su negocio.\n");
        prompt.append("Responde siempre en español, con un tono profesional pero muy amigable, usando emojis cuando sea apropiado.\n\n");
        
        prompt.append("--- DATOS ACTUALES DEL NEGOCIO ---\n");

        // Fetch sales of this user
        List<Sale> allSales = saleRepository.findByUserOrderByCreatedAtDesc(user);
        LocalDateTime now = LocalDateTime.now();
        
        BigDecimal salesThisMonth = allSales.stream()
                .filter(s -> s.getCreatedAt().getYear() == now.getYear() && s.getCreatedAt().getMonthValue() == now.getMonthValue())
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salesLastMonth = allSales.stream()
                .filter(s -> s.getCreatedAt().getYear() == now.minusMonths(1).getYear() && s.getCreatedAt().getMonthValue() == now.minusMonths(1).getMonthValue())
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        prompt.append("Total vendido este mes: $").append(salesThisMonth).append("\n");
        prompt.append("Total vendido el mes pasado: $").append(salesLastMonth).append("\n");

        // Fetch inventory of this user
        List<Product> products = productRepository.findByUser(user);
        List<Product> lowStock = products.stream().filter(p -> p.getStockQuantity() <= 5).collect(Collectors.toList());
        
        prompt.append("Total de productos en catálogo: ").append(products.size()).append("\n");
        prompt.append("Productos con stock bajo (<= 5): ");
        if (lowStock.isEmpty()) {
            prompt.append("Ninguno. Todo en orden.\n");
        } else {
            prompt.append("\n");
            for (Product p : lowStock) {
                prompt.append("- ").append(p.getName()).append(" (Stock: ").append(p.getStockQuantity()).append(")\n");
            }
        }
        prompt.append("----------------------------------\n");
        prompt.append("Si el usuario pregunta cómo usar el software, explícale que tiene un Panel (para ver alertas y ventas recientes), un POS (para registrar ventas), Finanzas (para costos fijos y variables), Inventario (para catálogos y auditorías) e Informes (para el libro mayor).\n");
        
        return prompt.toString();
    }
}
