# Spring Boot E-Commerce Backend - Complete Architecture Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Architecture & Flow](#architecture--flow)
3. [Technology Stack](#technology-stack)
4. [How Components Connect](#how-components-connect)
5. [Key Annotations Explained](#key-annotations-explained)
6. [Database & ORM Concepts](#database--orm-concepts)
7. [API Endpoints](#api-endpoints)
8. [Request-Response Lifecycle](#request-response-lifecycle)

---

## Overview

Your project is a **REST API backend** for an e-commerce application that allows users to:
- View all products
- View a single product
- Upload products with images
- Retrieve product images

The architecture follows the **Layered/Tier Architecture** pattern:
```
Client (Browser/Mobile App)
       ↓
   Controller Layer  ← Handles HTTP requests/responses
       ↓
   Service Layer     ← Business logic
       ↓
   Repository Layer  ← Database access
       ↓
   Database (H2)     ← Data storage
```

---

## Architecture & Flow

### **Layer 1: Controller Layer** (`ProductController.java`)
**Purpose**: Handle incoming HTTP requests and send responses back to clients.

**How it works**:
- Receives HTTP requests (GET, POST, etc.)
- Parses request data (path variables, request body, file uploads)
- Calls business logic from the Service layer
- Sends HTTP responses back (JSON, images, etc.)
- Handles HTTP status codes (200 OK, 201 CREATED, 404 NOT FOUND, 500 ERROR)

**Your Controllers**:
```
GET  /api/products              → Get all products
GET  /api/product/{id}          → Get single product by ID
POST /api/product               → Add new product with image
GET  /api/product/{productId}/image → Download product image
```

---

### **Layer 2: Service Layer** (`ProductService.java`)
**Purpose**: Contains business logic, data validation, and orchestration.

**How it works**:
- Receives data from the Controller
- Processes the business logic (e.g., converting image file to bytes)
- Validates data before saving
- Calls the Repository to access the database
- Returns processed data back to the Controller

**Your Service Methods**:
```
getAllProducts()           → Retrieves all products from database
getProductById(int id)     → Retrieves a specific product
addProduct(product, file)  → Saves product + converts image to binary
```

---

### **Layer 3: Repository Layer** (`ProductRepo.java`)
**Purpose**: Database access abstraction. Uses Spring Data JPA.

**How it works**:
- Extends `JpaRepository<Product, Integer>`
- Provides pre-built methods: `findAll()`, `findById()`, `save()`, `delete()`, etc.
- No need to write SQL queries for basic CRUD operations
- Spring automatically generates database queries

**What JpaRepository provides** (out of the box):
```
save(entity)              → INSERT or UPDATE
findAll()                 → SELECT * 
findById(id)              → SELECT WHERE id = ?
delete(entity)            → DELETE
count()                   → COUNT(*)
... and many more
```

---

### **Layer 4: Model/Entity Layer** (`Product.java`)
**Purpose**: Defines the database table structure as a Java class.

**How it works**:
- Each class with `@Entity` annotation = one database table
- Each field with `@Id` = primary key
- Each private field = one database column
- JPA/Hibernate automatically creates/updates the table

---

### **Layer 5: Database** (H2 In-Memory)
**Purpose**: Stores all data persistently (during runtime).

**Current Setup**:
- H2 embedded database (in-memory)
- URL: `jdbc:h2:mem:ecomdb`
- Automatically creates tables when application starts
- Data is lost when application stops

---

## Technology Stack

### **Dependencies** (from `pom.xml`):

1. **spring-boot-starter-data-jpa**
   - Provides JPA (Java Persistence API) interface
   - Enables Hibernate ORM (Object-Relational Mapping)
   - What it does: Converts Java objects to database records

2. **spring-boot-starter-web**
   - Provides REST API capabilities
   - Includes embedded Tomcat server
   - Handles HTTP requests/responses
   - What it does: Makes your application a web server

3. **spring-boot-devtools**
   - Auto-restarts application when files change
   - Faster development cycle

4. **h2** (H2 Database)
   - Lightweight, in-memory database
   - Good for learning and testing
   - What it does: Stores your product data

5. **lombok**
   - Reduces boilerplate Java code
   - Auto-generates getters, setters, constructors
   - Annotations: `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`

---

## How Components Connect

### **Example: GET /api/products Request Flow**

```
1. CLIENT sends: GET http://localhost:8080/api/products
        ↓
2. TOMCAT SERVLET (embedded web server) receives request
        ↓
3. SPRING DISPATCHER detects @GetMapping("/products")
        ↓
4. CONTROLLER executes: getAllProducts()
        ├─ Calls: service.getAllProducts()
        │
5. SERVICE executes: getAllProducts()
        ├─ Calls: repo.findAll()
        │
6. REPOSITORY (JpaRepository) executes: findAll()
        ├─ Generates SQL: SELECT * FROM product
        ├─ Sends to DATABASE
        │
7. DATABASE (H2) returns: List of all Product records
        ↓
8. REPOSITORY converts SQL results → List<Product> objects
        ↓
9. SERVICE returns this List to Controller
        ↓
10. CONTROLLER wraps in ResponseEntity with HTTP 200 OK
        ↓
11. SPRING JACKSON (JSON serializer) converts List<Product> → JSON
        ↓
12. CLIENT receives JSON response:
    [
      { "id": 1, "name": "Laptop", "price": 79999.99, ... },
      { "id": 2, "name": "Phone", "price": 49999.00, ... }
    ]
```

---

### **Example: POST /api/product Request Flow (with Image Upload)**

```
1. CLIENT sends: POST http://localhost:8080/api/product
   With FormData:
   - product: { "name": "Laptop", "brand": "Dell", "price": 79999.99, ... }
   - imageFile: <binary image data>
        ↓
2. SPRING MULTIPART HANDLER parses the FormData
        ↓
3. CONTROLLER executes: addProduct(Product, MultipartFile)
        ├─ Calls: service.addProduct(product, imageFile)
        │
4. SERVICE executes: addProduct(product, imageFile)
        ├─ Extracts: imageFile.getOriginalFilename() → stores name
        ├─ Extracts: imageFile.getContentType() → stores MIME type
        ├─ Extracts: imageFile.getBytes() → converts file to byte[]
        ├─ Sets: product.setImageName(name)
        ├─ Sets: product.setImageType(contentType)
        ├─ Sets: product.setImageData(bytes)
        ├─ Calls: repo.save(product)
        │
5. REPOSITORY executes: save(product)
        ├─ Generates SQL: INSERT INTO product (name, brand, ..., image_data, image_name, image_type)
        │                 VALUES (...)
        ├─ Sends to DATABASE
        │
6. DATABASE stores the Product record
        ↓
7. REPOSITORY returns: saved Product object (now with auto-generated ID)
        ↓
8. SERVICE returns this Product to Controller
        ↓
9. CONTROLLER returns ResponseEntity with HTTP 201 CREATED
        ↓
10. CLIENT receives:
    {
      "id": 1,
      "name": "Laptop",
      "brand": "Dell",
      "price": 79999.99,
      "imageName": "laptop.jpg",
      "imageType": "image/jpeg",
      ... (imageData NOT included due to @JsonIgnore)
    }
```

---

## Key Annotations Explained

### **Controller Annotations**

#### `@RestController`
```java
@RestController
public class ProductController {  }
```
- Tells Spring: "This class handles REST API requests"
- Every method returns JSON/data automatically (not HTML templates)
- Combines `@Controller` + `@ResponseBody`

#### `@RequestMapping("/api")`
```java
@RequestMapping("/api")
public class ProductController {  }
```
- Base URL prefix for all endpoints in this controller
- All endpoints start with `/api/`
- Example: Full URL becomes `/api/products`

#### `@CrossOrigin`
```java
@CrossOrigin
public class ProductController {  }
```
- Allows requests from different domains/ports
- Without this: Frontend on port 3000 cannot call backend on port 8080
- CORS = Cross-Origin Resource Sharing

#### `@GetMapping("/products")`
```java
@GetMapping("/products")
public ResponseEntity<List<Product>> getAllProducts() {  }
```
- Maps HTTP GET requests to `/api/products` to this method
- Similar: `@PostMapping`, `@PutMapping`, `@DeleteMapping`

#### `@PostMapping(value = "/product", consumes = {"multipart/form-data"})`
```java
@PostMapping(value = "/product", consumes = {"multipart/form-data"})
public ResponseEntity<?> addProduct() {  }
```
- Maps HTTP POST requests to `/api/product`
- `consumes = {"multipart/form-data"}` means: Accept file uploads (FormData)
- Without this: Cannot accept image files

#### `@PathVariable`
```java
@GetMapping("/product/{id}")
public ResponseEntity<Product> getProduct(@PathVariable int id) {  }
```
- Extracts URL parameter `{id}` as a Java variable
- Example: `/product/5` → `id = 5`

#### `@RequestPart`
```java
@PostMapping("/product")
public ResponseEntity<?> addProduct(
    @RequestPart Product product,
    @RequestPart MultipartFile imageFile
) {  }
```
- Extracts named form fields from multipart request
- `@RequestPart Product` → Extract form field named "product"
- `@RequestPart MultipartFile` → Extract file upload named "imageFile"

---

### **Entity/Model Annotations**

#### `@Entity`
```java
@Entity
public class Product {  }
```
- Tells JPA: "This class represents a database table"
- Table name = class name (lowercase by default)
- So `Product` class → `product` table

#### `@Id`
```java
@Id
private int id;
```
- Marks this field as PRIMARY KEY
- Must be unique for each record
- Used to uniquely identify a product

#### `@GeneratedValue(strategy = GenerationType.IDENTITY)`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;
```
- Auto-generates ID value when new product is inserted
- `IDENTITY` = Database auto-increment (incrementing numbers)
- You don't set ID manually; database does it

#### `@Lob` (Large Object)
```java
@Lob
private byte[] imageData;
```
- Tells JPA: This field stores large binary data
- `byte[]` = array of bytes (binary image data)
- Stores in BLOB column in database

#### `@Basic(fetch = FetchType.LAZY)`
```java
@Lob
@Basic(fetch = FetchType.LAZY)
private byte[] imageData;
```
- `LAZY` = Don't load this field by default
- When you fetch a Product, imageData is NOT loaded (saves memory/bandwidth)
- Load imageData only when explicitly accessed
- Good for large binary data

#### `@JsonIgnore`
```java
@JsonIgnore
private byte[] imageData;
```
- When converting Product to JSON: skip this field
- Why: Don't send huge binary data in API response
- Instead: Provide separate `/image` endpoint to download

#### `@JsonIgnoreProperties`
```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {  }
```
- Ignores Hibernate proxy fields when serializing to JSON
- Prevents JSON serialization errors

#### `@Data` (Lombok)
```java
@Data
public class Product {  }
```
- Auto-generates: getters, setters, toString(), equals(), hashCode()
- Reduces boilerplate code
- Combines: `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode`

#### `@AllArgsConstructor` (Lombok)
```java
@AllArgsConstructor
public class Product {  }
```
- Auto-generates constructor with all fields as parameters
- Example: `new Product(id, name, brand, price, ...)`

#### `@NoArgsConstructor` (Lombok)
```java
@NoArgsConstructor
public class Product {  }
```
- Auto-generates empty constructor
- Example: `new Product()`

---

### **Service Annotations**

#### `@Service`
```java
@Service
public class ProductService {  }
```
- Tells Spring: "This is a service class (business logic)"
- Spring automatically creates an instance (bean) of this class
- Can be injected into other classes

#### `@Autowired`
```java
@Autowired
private ProductService service;
```
- Tells Spring: "Automatically inject an instance of ProductService"
- Dependency Injection: Spring finds and provides the object
- Don't use `new ProductService()` manually

---

### **Repository Annotations**

#### `@Repository`
```java
@Repository
public interface ProductRepo extends JpaRepository<Product, Integer> {  }
```
- Tells Spring: "This is a data access object"
- Enables automatic SQL generation
- `<Product, Integer>` = Entity type, Primary Key type

---

## Database & ORM Concepts

### **What is ORM (Object-Relational Mapping)?**

ORM bridges Java objects and database tables:

```
Java World              Database World
─────────────           ──────────────
Product class      ←→   product table
Product object     ←→   product row
id field           ←→   id column
name field         ←→   name column
imageData field    ←→   image_data column
```

**Benefits**:
- Write Java code instead of SQL queries
- Database agnostic (switch from H2 to MySQL easily)
- Automatic type conversion (Java types ↔ SQL types)

### **What is Hibernate?**

Hibernate is the ORM framework that:
1. Generates SQL queries from your Java code
2. Converts database records to Java objects
3. Manages entity lifecycle (new, managed, detached, removed)
4. Handles relationships between entities

**Example**: Instead of writing:
```sql
SELECT * FROM product WHERE id = 1;
```

You write:
```java
Product product = repo.findById(1).orElse(null);
```

Hibernate generates the SQL for you!

### **JPA vs Hibernate**

- **JPA** = Standard interface/specification (like a contract)
- **Hibernate** = Implementation of JPA (the actual implementation)
- **Spring Data JPA** = Wrapper around Hibernate that makes it even easier

### **Database Table Structure** (Auto-generated)

Your `Product` class creates this table in H2:

```sql
CREATE TABLE product (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(255),
    desc            VARCHAR(255),
    brand           VARCHAR(255),
    price           DECIMAL(19,2),
    category        VARCHAR(255),
    release_date    TIMESTAMP,
    available       BOOLEAN,
    quantity        INT,
    image_name      VARCHAR(255),
    image_type      VARCHAR(255),
    image_data      BLOB            -- Binary Large Object (stores image bytes)
);
```

**How**: 
- `spring.jpa.hibernate.ddl-auto=update` tells Hibernate to automatically create/update tables
- Field names map to column names (camelCase → snake_case)

---

## API Endpoints

### **1. GET /api/products**
**Purpose**: Retrieve all products

**Request**:
```http
GET /api/products HTTP/1.1
Host: localhost:8080
```



**Backend Flow**:
1. Controller receives GET request
2. Calls `service.getAllProducts()`
3. Service calls `repo.findAll()`
4. Repository generates: `SELECT * FROM product`
5. Returns List<Product>
6. Jackson converts to JSON
7. Returns HTTP 200 OK with JSON body

---

### **2. GET /api/product/{id}**
**Purpose**: Retrieve a single product by ID

**Request**:
```http
GET /api/product/1 HTTP/1.1
Host: localhost:8080
```



**Backend Flow**:
1. `@PathVariable int id` extracts `1` from URL
2. Controller calls `service.getProductById(1)`
3. Service calls `repo.findById(1)`
4. Repository generates: `SELECT * FROM product WHERE id = 1`
5. If found: returns Product object → HTTP 200 OK
6. If NOT found: returns null → HTTP 404 Not Found

---

### **3. POST /api/product**
**Purpose**: Add new product with image

**Request**:
```http
POST /api/product HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="product"
Content-Type: application/json

{
  "name": "Laptop",
  "desc": "High performance",
  "brand": "Dell",
  "price": 79999.99,
  "category": "Electronics",
  "releaseDate": "2025-01-15",
  "available": true,
  "quantity": 10
}
--boundary
Content-Disposition: form-data; name="imageFile"; filename="laptop.jpg"
Content-Type: image/jpeg

[binary image data here]
--boundary--
```

**Backend Flow**:
1. `@RequestPart Product product` deserializes JSON → Product object
2. `@RequestPart MultipartFile imageFile` receives uploaded file
3. Controller calls `service.addProduct(product, imageFile)`
4. Service extracts image metadata:
   - `imageFile.getOriginalFilename()` → "laptop.jpg"
   - `imageFile.getContentType()` → "image/jpeg"
   - `imageFile.getBytes()` → binary image data
5. Service sets these on Product object
6. Service calls `repo.save(product)`
7. Repository generates: `INSERT INTO product (...) VALUES (...)`
8. Database assigns auto-generated ID
9. Returns saved Product with ID
10. HTTP 201 CREATED response

---

### **4. GET /api/product/{productId}/image**
**Purpose**: Download product image

**Request**:
```http
GET /api/product/1/image HTTP/1.1
Host: localhost:8080
```

**Response** (HTTP 200 OK):
```
Content-Type: image/jpeg
Content-Length: 15234

[binary image data here]
```

**Backend Flow**:
1. `@PathVariable int productId` extracts `1` from URL
2. Controller calls `service.getProductById(1)`
3. Service queries database
4. Controller extracts: `product.getImageData()` (byte array)
5. Controller sets HTTP response header: `Content-Type: image/jpeg`
6. Returns binary data
7. Browser displays/downloads image

---

## Request-Response Lifecycle

### **Complete Lifecycle Example: Create Product with Image**

```
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: CLIENT Sends Request                                        │
└─────────────────────────────────────────────────────────────────────┘

User sends:
  POST /api/product
  FormData {
    product: { name: "Laptop", brand: "Dell", price: 79999.99 },
    imageFile: <image_file>
  }

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Spring Web Server (Tomcat) Receives Request                 │
└─────────────────────────────────────────────────────────────────────┘

Tomcat identifies:
  - HTTP Method: POST
  - URL: /api/product
  - Body: Multipart form data

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: Spring Dispatcher Maps to Controller                        │
└─────────────────────────────────────────────────────────────────────┘

Spring finds:
  @PostMapping("/product") 
  in ProductController
  
↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Multipart Parser Extracts Data                              │
└─────────────────────────────────────────────────────────────────────┘

Extracts:
  - "product" field → Jackson deserializes JSON → Product object
  - "imageFile" field → MultipartFile object (wrapper around uploaded file)

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: Controller Method Executes                                   │
└─────────────────────────────────────────────────────────────────────┘

ProductController.addProduct(Product, MultipartFile) {
    service.addProduct(product, imageFile);
}

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 6: Service Layer - Business Logic                              │
└─────────────────────────────────────────────────────────────────────┘

ProductService.addProduct(Product, MultipartFile) {
    product.setImageName(imageFile.getOriginalFilename());
    product.setImageType(imageFile.getContentType());
    product.setImageData(imageFile.getBytes());  // Convert file to bytes
    return repo.save(product);  // Call repository
}

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 7: Repository Layer - Database Access (Hibernate)              │
└─────────────────────────────────────────────────────────────────────┘

ProductRepo.save(Product) {
    // Hibernate generates SQL:
    INSERT INTO product 
    (name, brand, price, image_name, image_type, image_data, ...)
    VALUES (?, ?, ?, ?, ?, ?, ...)
}

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 8: Database Execution                                          │
└─────────────────────────────────────────────────────────────────────┘

H2 Database:
  - Executes INSERT query
  - Auto-generates ID (e.g., id = 1)
  - Stores row in product table
  - Returns success

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 9: Return Data to Service                                      │
└─────────────────────────────────────────────────────────────────────┘

Hibernate converts:
  - Database row → Product Java object
  - Now includes auto-generated ID
  - Returns to Service layer

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 10: Service Returns to Controller                              │
└─────────────────────────────────────────────────────────────────────┘

Service returns:
  Product { id: 1, name: "Laptop", brand: "Dell", ... }

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 11: Controller Wraps Response                                  │
└─────────────────────────────────────────────────────────────────────┘

Controller returns:
  ResponseEntity.status(201).body(savedProduct)
  
HTTP Status: 201 CREATED

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 12: Jackson Serializes to JSON                                 │
└─────────────────────────────────────────────────────────────────────┘

Jackson converts Product object:
  Product { id: 1, name: "Laptop", ... }
  ↓
  {
    "id": 1,
    "name": "Laptop",
    "brand": "Dell",
    "price": 79999.99,
    "imageName": "laptop.jpg",
    "imageType": "image/jpeg"
    // imageData NOT included (@JsonIgnore)
  }

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 13: HTTP Response Sent to Client                              │
└─────────────────────────────────────────────────────────────────────┘

HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 1,
  "name": "Laptop",
  "brand": "Dell",
  ...
}

↓

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 14: Client Receives Response                                   │
└─────────────────────────────────────────────────────────────────────┘

Frontend receives:
  - HTTP Status: 201 (Success - Resource Created)
  - JSON body with created product data
  - Can now use this product ID for future requests
```

---

## Summary: How Everything Works Together

### **Request Path** (What happens when you call an API):
```
HTTP Request
    ↓
Tomcat (Web Server)
    ↓
Spring Dispatcher (Router)
    ↓
Controller (Request Handler)
    ↓
Service (Business Logic)
    ↓
Repository (Database Access)
    ↓
Hibernate (ORM - Object to SQL)
    ↓
H2 Database (SQL Execution & Storage)
    ↓
[Data returned back up the chain]
    ↓
Jackson (Object to JSON)
    ↓
HTTP Response
    ↓
Client (Browser/App)
```

### **Key Concepts Recap**:

1. **@RestController** = Handles REST requests
2. **@RequestMapping** = URL prefix
3. **@GetMapping/@PostMapping** = HTTP method + URL routing
4. **@Autowired** = Dependency Injection (automatic wiring)
5. **@Entity** = Database table
6. **@Id @GeneratedValue** = Primary Key with auto-increment
7. **@Lob** = Large binary data (images)
8. **@Lazy** = Load data only when needed
9. **@JsonIgnore** = Don't include in JSON response
10. **JpaRepository** = Pre-built database access methods
11. **Service** = Business logic layer
12. **DTO** = Data Transfer Object (if needed for specific responses)

---

## Next Steps for Learning

1. **Try modifying**: Add a DELETE endpoint (`@DeleteMapping`)
2. **Try adding**: An UPDATE endpoint (`@PutMapping`)
3. **Try querying**: Add custom search methods in ProductRepo
4. **Try filtering**: Add category filter to getAllProducts
5. **Try pagination**: Use `Page<Product>` instead of `List<Product>`
6. **Try validation**: Add `@Valid` and validation annotations
7. **Try error handling**: Create custom exceptions and error handlers

---

**Your project is now ready for these enhancements!**


