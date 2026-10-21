package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.Inventory;
import com.example.ERP_Api.Entity.Product;
import com.example.ERP_Api.Repository.InventoryRepository;
import com.example.ERP_Api.Repository.OrderRepository;
import com.example.ERP_Api.Repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    public Page<Product> list(String search, String category, Pageable pageable) {
        if (search != null && category != null) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(
                    search, category, pageable);
        }
        if (search != null) {
            return productRepository.findByNameContainingIgnoreCase(search, pageable);
        }
        return productRepository.findAll(pageable);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public Product create(Product product) {
        Product saved = productRepository.save(product);
        inventoryRepository.save(new Inventory(saved, 0)); // starts at 0, adjust stock separately
        return saved;
    }

    public Product update(Long id, Product changes) {
        Product existing = getById(id);
        if (changes.getName() != null) existing.setName(changes.getName());
        if (changes.getSku() != null) existing.setSku(changes.getSku());
        if (changes.getCategory() != null) existing.setCategory(changes.getCategory());
        if (changes.getPriceCents() != null) existing.setPriceCents(changes.getPriceCents());
        return productRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getById(id); // 404 if it doesn't exist

        if (orderRepository.existsByProductId(id)) {
            throw new IllegalStateException(
                    "Cannot delete \"" + product.getName() + "\": it is used by existing orders.");
        }

        // Remove its inventory row first (stock history is removed with it)
        inventoryRepository.findByProductId(id).ifPresent(inventoryRepository::delete);
        productRepository.delete(product);
    }
}