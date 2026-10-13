# Stocky - Backend (Java / Spring Boot)

REST API server for the Stocky stock, inventory, and point-of-sale management system.

## Technologies Used
* Java 17+
* Spring Boot (Spring Web, Spring Data JPA)
* PostgreSQL (Relational database)
* Maven (Dependency manager and build)

## Main Modules
* Authentication and Users: User management and access control (`AuthController`, `UserController`).

* Inventory and Products: CRUD operations for catalog, categories, prices, and minimum stock levels (`ProductController`).

* Point of Sale and Sales: Order processing and cash register transactions (`SalesController`). * **Finance and Reporting:** Sales, balance, and expense metrics (`FinanceController`, `ReportController`).

## Configuration and Execution
1. Configure the database in `src/main/resources/application.properties`:

``properties

spring.datasource.url=jdbc:postgresql://localhost:5432/stocky

spring.datasource.username=postgres

spring.datasource.password=root

