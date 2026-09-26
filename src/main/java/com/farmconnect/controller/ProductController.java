package com.farmconnect.controller;

import com.farmconnect.dto.request.ProductRequest;
import com.farmconnect.dto.response.ProductResponse;
import com.farmconnect.entity.Product;
import com.farmconnect.service.impl.FileStorageServiceImpl;
import com.farmconnect.service.impl.ProductServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductServiceImpl productService;
    private final FileStorageServiceImpl fileStorageService;

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAllAvailable();
    }

    @GetMapping("/{id}")
    public ProductResponse getOne(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping("/category/{category}")
    public List<ProductResponse> byCategory(@PathVariable Product.Category category) {
        return productService.getByCategory(category);
    }

    @GetMapping("/organic")
    public List<ProductResponse> organic() {
        return productService.getOrganic();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam String q) {
        return productService.search(q);
    }

    @GetMapping("/recent")
    public List<ProductResponse> recent() {
        return productService.recentlyAdded();
    }

    @GetMapping("/mine")
    public List<ProductResponse> mine() {
        return productService.myProducts();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.addProduct(req));
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest req) {
        return productService.updateProduct(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ProductResponse uploadImage(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {
        String url = fileStorageService.store(file);
        return productService.setImage(id, url);
    }

    @PatchMapping("/{id}/availability")
    public ProductResponse toggleAvailability(@PathVariable Long id) {
        return productService.toggleAvailability(id);
    }
}
