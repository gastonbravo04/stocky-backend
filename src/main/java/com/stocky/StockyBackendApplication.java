package com.stocky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockyBackendApplication {

    @jakarta.annotation.PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    public static void main(String[] args) {
        SpringApplication.run(StockyBackendApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner initData(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("UPDATE expenses SET category = 'PROVEEDORES' WHERE category = 'SUPPLIER'");
                jdbcTemplate.execute("UPDATE expenses SET category = 'SERVICIOS' WHERE category = 'SERVICES'");
                jdbcTemplate.execute("UPDATE expenses SET category = 'MERCADERIA' WHERE category = 'PACKAGING'");
                jdbcTemplate.execute("UPDATE expenses SET category = 'SUELDOS' WHERE category = 'SALARY'");
                jdbcTemplate.execute("UPDATE expenses SET category = 'OTROS' WHERE category = 'OTHER'");
                System.out.println("Migración de categorías de gastos completada con éxito.");
            } catch (Exception e) {
                System.err.println("Error al migrar categorías: " + e.getMessage());
            }
        };
    }
}
