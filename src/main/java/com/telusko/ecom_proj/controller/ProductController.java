package com.telusko.ecom_proj.controller;

import com.telusko.ecom_proj.model.Product;
import com.telusko.ecom_proj.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts()
    {
        return new ResponseEntity<>(service.getAllProducts(), HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable int id)
    {
        Product product = service.getProductById(id);
        if (product != null) {
            return new ResponseEntity<>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/product", consumes = {"multipart/form-data"})
    public ResponseEntity<?> addProduct(@RequestPart Product product,
                                        @RequestPart MultipartFile imageFile)
    {
        try{
            Product savedProduct = service.addProduct(product, imageFile);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error adding product: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/product/{productId}/image")
    public ResponseEntity<byte[]> getImageByProductId(@PathVariable int productId) {
        Product product = service.getProductById(productId);
        if (product == null || product.getImageData() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(product.getImageType()))
                .body(product.getImageData());
    }

    @PutMapping(value = "/product/{id}", consumes = "multipart/form-data")
    public ResponseEntity<String> updateProduct(@PathVariable int id, @RequestPart Product product,
                                                @RequestPart(required = false) MultipartFile imageFile) {
        try {
            Product updatedProduct = service.updateProduct(id, product, imageFile);
            if (updatedProduct != null) {
                return new ResponseEntity<>("Product updated successfully", HttpStatus.OK);
            }
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            return new ResponseEntity<>("Error updating product: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable int id) {
            Product product  = service.getProductById(id);
            if(product != null) {
                service.deleteProduct(id);
                return new ResponseEntity<>("Product deleted successfully", HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
            }
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        System.out.println("Searching products with keyword: " + keyword);
        List<Product> products = service.searchProducts(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

}
