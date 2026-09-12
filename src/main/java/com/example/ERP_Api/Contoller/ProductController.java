package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Entity.Product;
import com.example.ERP_Api.Service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<Product> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return productService.list(search, category, pageable);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product) {
        return productService.create(product);
    }

    @PatchMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product changes) {
        return productService.update(id, changes);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
