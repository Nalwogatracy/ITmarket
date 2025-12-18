package com.IT_market.controller;

import com.IT_market.dto.ProductRequest;
import com.IT_market.model.Product;
import com.IT_market.model.User;
import com.IT_market.security.AuthInterceptor;
import com.IT_market.service.ProductService;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                    ? Sort.by(sortBy).descending() 
                    : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Product> products = productService.getProducts(
                    pageable, category, minPrice, maxPrice, search);
            
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching products: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Product> products = productService.getFeaturedProducts(pageable);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching featured products: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch featured products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(product);
            
        } catch (RuntimeException e) {
            System.err.println("Product not found: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Product> products = productService.getProductsByCategory(category, pageable);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching products by category: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String q) {
        try {
            List<Product> products = productService.searchProducts(q);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error searching products: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to search products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null || !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Product> products = productService.getLowStockProducts(threshold);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching low stock products: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch low stock products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/top-selling")
    public ResponseEntity<?> getTopSellingProducts(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Product> products = productService.getTopSellingProducts(limit);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching top selling products: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch top selling products");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/new-arrivals")
    public ResponseEntity<?> getNewArrivals(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Product> products = productService.getNewArrivals(limit);
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            System.err.println("Error fetching new arrivals: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch new arrivals");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Product.ProductCategory> categories = productService.getAllCategories();
            return ResponseEntity.ok(categories);
            
        } catch (Exception e) {
            System.err.println("Error fetching categories: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch categories");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/brands")
    public ResponseEntity<?> getAllBrands() {
        try {
            List<String> brands = productService.getAllBrands();
            return ResponseEntity.ok(brands);
            
        } catch (Exception e) {
            System.err.println("Error fetching brands: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch brands");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // In your ProductController, fix the createProduct method:

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody ProductRequest request) {

        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Check if user is seller or admin
            boolean isSellerOrAdmin = currentUser.isSeller() || currentUser.isAdmin();

            if (!isSellerOrAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Seller or admin access required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Use Long userId (matching your User entity)
            Product product = productService.createProduct(request, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Product created successfully");
            response.put("productId", product.getId());
            response.put("productName", product.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("Error creating product: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create product");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
        @PutMapping("/{id}")
        public ResponseEntity<?> updateProduct(
                @RequestHeader("Authorization") String token,
                @PathVariable String id,
                @RequestBody ProductRequest request) {

            try {
                User currentUser = AuthInterceptor.getCurrentUser();
                if (currentUser == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                Product product = productService.getProductById(id);

                // Check if user owns the product or is admin
                boolean isOwner = product.getSeller() != null && 
                                 product.getSeller().getId().equals(currentUser.getId());
                boolean isAdmin = currentUser.isAdmin();

                if (!isOwner && !isAdmin) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Forbidden");
                    error.put("message", "You don't have permission to update this product");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
                }

                Product updatedProduct = productService.updateProduct(id, request);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Product updated successfully");
                response.put("productId", updatedProduct.getId());
                response.put("productName", updatedProduct.getName());

                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                System.err.println("Error updating product: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product not found");
                error.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            } catch (Exception e) {
                System.err.println("Error updating product: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to update product");
                error.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Product product = productService.getProductById(id);
            
            // Only admin or product owner can delete
            boolean isOwner = product.getSeller() != null && 
                             product.getSeller().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.isAdmin();
            
            if (!isOwner && !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "You don't have permission to delete this product");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            productService.deleteProduct(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.err.println("Error deleting product: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete product");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{id}/images")
    public ResponseEntity<?> uploadProductImage(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "primary", defaultValue = "false") boolean primary) {
        
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Product product = productService.getProductById(id);
            
            // Check if user owns the product or is admin
            boolean isOwner = product.getSeller() != null && 
                             product.getSeller().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.isAdmin();
            
            if (!isOwner && !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "You don't have permission to update this product");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            String imageUrl = productService.addProductImage(id, file, primary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Image uploaded successfully");
            response.put("imageUrl", imageUrl);
            response.put("productId", id);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            System.err.println("Error uploading image: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (RuntimeException e) {
            System.err.println("Product not found: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.err.println("Error uploading image: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PutMapping("/{id}/toggle-featured")
    public ResponseEntity<?> toggleFeatured(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null || !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            productService.toggleFeatured(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product featured status toggled");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("Product not found: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.err.println("Error toggling featured status: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to toggle featured status");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Product product = productService.getProductById(id);
            
            // Check if user owns the product or is admin
            boolean isOwner = product.getSeller() != null && 
                             product.getSeller().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.isAdmin();
            
            if (!isOwner && !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "You don't have permission to update this product");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            productService.toggleActive(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product active status toggled");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("Product not found: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.err.println("Error toggling active status: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to toggle active status");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getProductStats() {
        try {
            User currentUser = AuthInterceptor.getCurrentUser();
            if (currentUser == null || !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            long totalProducts = productService.getTotalProducts();
            long activeProducts = productService.getActiveProductsCount();
            BigDecimal inventoryValue = productService.getTotalInventoryValue();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProducts", totalProducts);
            stats.put("activeProducts", activeProducts);
            stats.put("inactiveProducts", totalProducts - activeProducts);
            stats.put("inventoryValue", inventoryValue);
            stats.put("categoriesCount", productService.getAllCategories().size());
            stats.put("brandsCount", productService.getAllBrands().size());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("Error fetching product stats: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch product statistics");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}