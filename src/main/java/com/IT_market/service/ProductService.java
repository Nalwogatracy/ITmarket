package com.IT_market.service;

import com.IT_market.dto.ProductRequest;
import com.IT_market.model.Product;
import com.IT_market.model.ProductImage;
import com.IT_market.model.User;
import com.IT_market.repository.ProductRepository;
import com.IT_market.repository.UserRepository;
import static io.lettuce.core.KillArgs.Builder.id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.IT_market.service.CloudinaryService;

@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    
    
    public ProductService(ProductRepository productRepository,
                        CloudinaryService cloudinaryService,
                     UserRepository userRepository,
                     FileStorageService fileStorageService) {
    this.productRepository = productRepository;
    this.cloudinaryService = cloudinaryService; // ← ADD THIS LINE
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
    }
    
    @Transactional
    public void updateProductViews(String productId) {
        try {
            // Increment view count using repository method
            productRepository.incrementViewCount(productId);
            
            // Optional: Get the updated product to log the view count
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                System.out.println("Product viewed: " + productId + 
                                 " - " + product.getName() + 
                                 " (Total views: " + product.getViewCount() + ")");
                
                // Log view analytics (could be expanded to track user, session, etc.)
                logViewAnalytics(productId);
            }
        } catch (Exception e) {
            System.err.println("Error updating view count for product: " + productId);
            e.printStackTrace();
        }
    }
    
    @Transactional(readOnly = true)
    public Product getProductWithViews(String productId) {
        Product product = getProductById(productId);
        
        // Increment view count when product is retrieved (for detailed view)
        productRepository.incrementViewCount(productId);
        
        return product;
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getMostViewedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewCount").descending());
        return productRepository.findMostViewedProducts(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getTrendingProducts(int days, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("viewCount").descending());
        return productRepository.findTrendingProducts(days, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int threshold) {
        return productRepository.findPopularProducts(threshold);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getViewStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get total views across all products
            Long totalViews = productRepository.getTotalViews();
            stats.put("totalViews", totalViews != null ? totalViews : 0);
            
            // Get average views per product
            Double averageViews = productRepository.getAverageViews();
            stats.put("averageViews", averageViews != null ? String.format("%.2f", averageViews) : "0.00");
            
            // Get most viewed product
            List<Product> topProducts = productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .sorted((p1, p2) -> {
                        int views1 = p1.getViewCount() != null ? p1.getViewCount() : 0;
                        int views2 = p2.getViewCount() != null ? p2.getViewCount() : 0;
                        return Integer.compare(views2, views1);
                    })
                    .limit(5)
                    .toList();
            
            stats.put("topViewedProducts", topProducts);
            
            // Get view distribution by category
            Map<String, Long> viewsByCategory = new HashMap<>();
            productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .forEach(product -> {
                        String category = product.getCategory();
                        int views = product.getViewCount() != null ? product.getViewCount() : 0;
                        viewsByCategory.put(category, viewsByCategory.getOrDefault(category, 0L) + views);
                    });
            
            stats.put("viewsByCategory", viewsByCategory);
            
            // Get today's views (simplified - in production you'd have a separate view tracking table)
            long todayViews = productRepository.findAll().stream()
                    .filter(product -> product.getUpdatedAt() != null && 
                            product.getUpdatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                    .mapToLong(product -> product.getViewCount() != null ? product.getViewCount() : 0)
                    .sum();
            
            stats.put("todayViews", todayViews);
            
        } catch (Exception e) {
            System.err.println("Error getting view statistics: " + e.getMessage());
            stats.put("error", "Failed to get view statistics");
        }
        
        return stats;
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getProductViewAnalytics(String productId) {
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            Product product = getProductById(productId);
            
            analytics.put("productId", productId);
            analytics.put("productName", product.getName());
            analytics.put("totalViews", product.getViewCount() != null ? product.getViewCount() : 0);
            analytics.put("createdAt", product.getCreatedAt());
            analytics.put("lastUpdated", product.getUpdatedAt());
            
            // Calculate views per day (average)
            if (product.getCreatedAt() != null) {
                long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                        product.getCreatedAt().toLocalDate(), 
                        LocalDateTime.now().toLocalDate());
                
                if (daysSinceCreation > 0) {
                    double viewsPerDay = (double) product.getViewCount() / daysSinceCreation;
                    analytics.put("viewsPerDay", String.format("%.2f", viewsPerDay));
                } else {
                    analytics.put("viewsPerDay", product.getViewCount());
                }
            }
            
            // Get similar products for comparison
            List<Map<String, Object>> similarProducts = productRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(productId) && p.isActive())
                    .filter(p -> p.getCategory().equals(product.getCategory()))
                    .sorted((p1, p2) -> {
                        int views1 = p1.getViewCount() != null ? p1.getViewCount() : 0;
                        int views2 = p2.getViewCount() != null ? p2.getViewCount() : 0;
                        return Integer.compare(views2, views1);
                    })
                    .limit(3)
                    .map(p -> {
                        Map<String, Object> similar = new HashMap<>();
                        similar.put("id", p.getId());
                        similar.put("name", p.getName());
                        similar.put("views", p.getViewCount());
                        similar.put("rating", p.getRating());
                        return similar;
                    })
                    .toList();
            
            analytics.put("similarProducts", similarProducts);
            
        } catch (Exception e) {
            System.err.println("Error getting product view analytics: " + e.getMessage());
            analytics.put("error", "Failed to get analytics");
        }
        
        return analytics;
    }
    
    @Transactional
    public void resetViewCount(String productId) {
        Product product = getProductById(productId);
        product.setViewCount(0);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("View count reset for product: " + product.getName());
    }
    
    @Transactional
    public void batchUpdateViewCounts(Map<String, Integer> viewUpdates) {
        for (Map.Entry<String, Integer> entry : viewUpdates.entrySet()) {
            try {
                Product product = getProductById(entry.getKey());
                product.setViewCount(entry.getValue());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
            } catch (Exception e) {
                System.err.println("Error updating view count for product: " + entry.getKey());
            }
        }
        
        System.out.println("Batch updated view counts for " + viewUpdates.size() + " products");
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getViewLeaderboard(int limit) {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .sorted((p1, p2) -> {
                    int views1 = p1.getViewCount() != null ? p1.getViewCount() : 0;
                    int views2 = p2.getViewCount() != null ? p2.getViewCount() : 0;
                    return Integer.compare(views2, views1);
                })
                .limit(limit)
                .map(product -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("rank", 0); // Will be set later
                    entry.put("id", product.getId());
                    entry.put("name", product.getName());
                    entry.put("category", product.getCategory());
                    entry.put("views", product.getViewCount());
                    entry.put("soldCount", product.getSoldCount());
                    entry.put("rating", product.getRating());
                    entry.put("price", product.getPrice());
                    return entry;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Private helper method for view analytics
    private void logViewAnalytics(String productId) {
        // In a production system, you would:
        // 1. Log to a separate analytics table
        // 2. Track user ID, session, timestamp, etc.
        // 3. Use this data for recommendations
        
        // For now, we'll just log to console
        System.out.println("View logged at: " + LocalDateTime.now() + 
                          " for product: " + productId);
        
        // Example of what you could store in an analytics table:
        /*
        ViewAnalytics analytics = new ViewAnalytics();
        analytics.setProductId(productId);
        analytics.setViewedAt(LocalDateTime.now());
        analytics.setUserId(currentUserId); // If user is logged in
        analytics.setSessionId(sessionId);
        analytics.setIpAddress(ipAddress);
        analytics.setUserAgent(userAgent);
        analytics.setReferrer(referrer);
        // Save to analytics repository
        */
    }
   
    
    @Transactional(readOnly = true)
    public Page<Product> getProducts(Pageable pageable, String category, 
                                   BigDecimal minPrice, BigDecimal maxPrice,
                                   String search) {
        return productRepository.searchProducts(search, category, minPrice, maxPrice, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getFeaturedProducts(Pageable pageable) {
        return productRepository.findByFeaturedTrueAndActiveTrue(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategoryAndActiveTrue(category, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsByBrand(String brand, Pageable pageable) {
        return productRepository.findByBrandAndActiveTrue(brand, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getProductsBySeller(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return productRepository.findAll().stream()
                .filter(product -> sku.equals(product.getSku()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
    }
    
    @Transactional
    public Product createProduct(ProductRequest request, Long sellerId) {
        // Get seller from database
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found with id: " + sellerId));
        
        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setSubCategory(request.getSubCategory());
        product.setBrand(request.getBrand());
        product.setDescription(request.getDescription());
        product.setSpecifications(request.getSpecifications());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setMinimumOrderQuantity(request.getMinimumOrderQuantity() != null ? request.getMinimumOrderQuantity() : 1);
        product.setSku(request.getSku());
        product.setTags(request.getTags());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());
        product.setSeller(seller);
        
        // Set timestamps
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product savedProduct = productRepository.save(product);
        System.out.println("Product created: " + savedProduct.getName() + " by seller: " + seller.getUsername());
        
        return savedProduct;
    }
    
    @Transactional
    public Product updateProduct(String id, ProductRequest request) {
        Product product = getProductById(id);
        
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setSubCategory(request.getSubCategory());
        product.setBrand(request.getBrand());
        product.setDescription(request.getDescription());
        product.setSpecifications(request.getSpecifications());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : product.getStockQuantity());
        product.setMinimumOrderQuantity(request.getMinimumOrderQuantity() != null ? request.getMinimumOrderQuantity() : product.getMinimumOrderQuantity());
        product.setSku(request.getSku());
        product.setTags(request.getTags());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product updatedProduct = productRepository.save(product);
        System.out.println("Product updated: " + updatedProduct.getName());
        
        return updatedProduct;
    }
    
    @Transactional
    public void deleteProduct(String id) {
        Product product = getProductById(id);
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        System.out.println("Product soft deleted: " + id);
    }
    
    @Transactional
    public void permanentlyDeleteProduct(String id) {
        Product product = getProductById(id);
        productRepository.delete(product);
        System.out.println("Product permanently deleted: " + id);
    }
    
    @Transactional
    public void updateStock(String productId, Integer quantity) {
        Product product = getProductById(productId);
        int newStock = product.getStockQuantity() - quantity;
        
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(newStock);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Stock updated for product: " + product.getName() + 
                          ". New stock: " + newStock);
    }
    
    @Transactional
    public void increaseStock(String productId, Integer quantity) {
        Product product = getProductById(productId);
        int newStock = product.getStockQuantity() + quantity;
        product.setStockQuantity(newStock);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Stock increased for product: " + product.getName() + 
                          ". New stock: " + newStock);
    }
    
    @Transactional
    public void updateProductRating(String productId, Double newRating) {
        Product product = getProductById(productId);
        
        // Calculate new average rating
        double currentTotal = product.getRating() * product.getReviewCount();
        double newTotal = currentTotal + newRating;
        int newReviewCount = product.getReviewCount() + 1;
        double newAverageRating = newTotal / newReviewCount;
        
        product.setRating(newAverageRating);
        product.setReviewCount(newReviewCount);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Rating updated for product: " + product.getName() + 
                          ". New rating: " + newAverageRating);
    }
    
    @Transactional
    public String addProductImage(String productId, MultipartFile file, boolean isPrimary) throws IOException {
        Product product = getProductById(productId);
        
        // 1. Upload to Cloudinary (simple one-liner)
        String imageUrl = cloudinaryService.uploadProductImage(file);
        
        // 2. Create and save your ProductImage entity with the URL
        ProductImage image = new ProductImage();
        image.setImageUrl(imageUrl);
        image.setAltText(product.getName());
        image.setPrimary(isPrimary);
        image.setProduct(product);
        image.setDisplayOrder(product.getImages().size());
        
        product.addImage(image);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        // 3. Optional: If setting as primary, update other images
        if (isPrimary) {
            for (ProductImage otherImage : product.getImages()) {
                if (otherImage != image && otherImage.isPrimary()) {
                    otherImage.setPrimary(false);
                }
            }
        }
        
        System.out.println("Image uploaded to Cloudinary for product: " + product.getName() + ". URL: " + imageUrl);
        return imageUrl;
    }
    
    @Transactional
    public void removeProductImage(String productId, Long imageId) {
        Product product = getProductById(productId);
        
        ProductImage imageToRemove = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));
        
        // Delete from Cloudinary
        try {
            cloudinaryService.deleteImage(imageToRemove.getImageUrl());
            System.out.println("Image deleted from Cloudinary: " + imageToRemove.getImageUrl());
        } catch (IOException e) {
            System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
            // Continue with database removal even if Cloudinary delete fails
        }
        
        // Remove from database
        product.getImages().remove(imageToRemove);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Image record removed from database for product: " + product.getName());
    }
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query) {
        return productRepository.findAll().stream()
                .filter(product -> product.isActive() &&
                        (product.getName().toLowerCase().contains(query.toLowerCase()) ||
                         product.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                         product.getBrand() != null && product.getBrand().toLowerCase().contains(query.toLowerCase()) ||
                         product.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(query.toLowerCase()))))
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getTopSellingProducts(int limit) {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .sorted((p1, p2) -> Integer.compare(p2.getSoldCount(), p1.getSoldCount()))
                .limit(limit)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<Product> getNewArrivals(int limit) {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(limit)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public long getTotalProducts() {
        return productRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getActiveProductsCount() {
        return productRepository.countActiveProducts();
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalInventoryValue() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transactional
    public void toggleFeatured(String productId) {
        Product product = getProductById(productId);
        product.setFeatured(!product.isFeatured());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Product featured status toggled: " + product.getName() + 
                          ". Featured: " + product.isFeatured());
    }
    
    @Transactional
    public void toggleActive(String productId) {
        Product product = getProductById(productId);
        product.setActive(!product.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Product active status toggled: " + product.getName() + 
                          ". Active: " + product.isActive());
    }
    
    @Transactional
    public void incrementSoldCount(String productId, int quantity) {
        Product product = getProductById(productId);
        product.setSoldCount(product.getSoldCount() + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        
        System.out.println("Sold count incremented for product: " + product.getName() + 
                          ". New sold count: " + product.getSoldCount());
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(Product::getCategory)
                .distinct()
                .sorted()
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(Product::getBrand)
                .filter(brand -> brand != null && !brand.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .flatMap(product -> product.getTags().stream())
                .distinct()
                .sorted()
                .toList();
    }
}