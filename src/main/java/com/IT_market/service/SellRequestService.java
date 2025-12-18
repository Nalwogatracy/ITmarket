// File: src/main/java/com/IT_market/service/SellRequestService.java
package com.IT_market.service;

import com.IT_market.dto.SellRequestDTO;
import com.IT_market.model.User;
import com.IT_market.model.Product;
import com.IT_market.model.SellRequest;
import com.IT_market.repository.SellRequestRepository;
import com.IT_market.repository.UserRepository;
import com.IT_market.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SellRequestService {
    
    private final SellRequestRepository sellRequestRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    
    public SellRequestService(SellRequestRepository sellRequestRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              ProductService productService) {
        this.sellRequestRepository = sellRequestRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }
    
    // ============== CREATE & SUBMIT ==============
    
    @Transactional
    public SellRequest submitSellRequest(SellRequestDTO requestDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check for duplicate active requests
        if (sellRequestRepository.existsActiveRequestForUserAndItem(userId, requestDTO.getItemName())) {
            throw new RuntimeException("You already have an active sell request for this item. Please wait for it to be processed.");
        }
        
        SellRequest sellRequest = new SellRequest();
        sellRequest.setUser(user);
        sellRequest.setItemName(requestDTO.getItemName());
        sellRequest.setItemDescription(requestDTO.getItemDescription());
        sellRequest.setItemCategory(requestDTO.getItemCategory());
        sellRequest.setItemCondition(requestDTO.getItemCondition());
        sellRequest.setExpectedPrice(requestDTO.getExpectedPrice());
        sellRequest.setStatus(SellRequest.SellRequestStatus.PENDING);
        sellRequest.setSubmittedAt(LocalDateTime.now());
        
        // Save the request
        SellRequest savedRequest = sellRequestRepository.save(sellRequest);
        
        // Send notification
        sendNewRequestNotification(savedRequest);
        
        return savedRequest;
    }
    
    // ============== ADMIN OPERATIONS ==============
    
    @Transactional(readOnly = true)
    public List<SellRequest> getAllSellRequests() {
        return sellRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "submittedAt"));
    }
    
    @Transactional(readOnly = true)
    public Page<SellRequest> getAllSellRequests(Pageable pageable) {
        return sellRequestRepository.findAll(PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            Sort.by(Sort.Direction.DESC, "submittedAt")
        ));
    }
    
    @Transactional(readOnly = true)
    public Page<SellRequest> getAllSellRequests(SellRequest.SellRequestStatus status, Pageable pageable) {
        if (status != null) {
            return sellRequestRepository.findByStatus(status, pageable);
        }
        return getAllSellRequests(pageable);
    }
    
    @Transactional(readOnly = true)
    public List<SellRequest> getSellRequestsByStatus(SellRequest.SellRequestStatus status) {
        return sellRequestRepository.findByStatus(status);
    }
    
    @Transactional
    public SellRequest approveSellRequest(Long requestId, BigDecimal finalPrice, User adminUser) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        if (!sellRequest.isPending() && !sellRequest.isNegotiating()) {
            throw new RuntimeException("Cannot approve request with status: " + sellRequest.getStatus());
        }
        
        // Approve the request
        sellRequest.approve(finalPrice, adminUser);
        
        // Create a product from the approved request
        createProductFromSellRequest(sellRequest);
        
        SellRequest updatedRequest = sellRequestRepository.save(sellRequest);
        
        // Notify the seller
        sendApprovalNotification(updatedRequest);
        
        return updatedRequest;
    }
    
    @Transactional
    public SellRequest rejectSellRequest(Long requestId, String reason, User adminUser) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        if (!sellRequest.isPending() && !sellRequest.isNegotiating()) {
            throw new RuntimeException("Cannot reject request with status: " + sellRequest.getStatus());
        }
        
        // Reject the request
        sellRequest.reject(reason, adminUser);
        
        SellRequest updatedRequest = sellRequestRepository.save(sellRequest);
        
        // Notify the seller
        sendRejectionNotification(updatedRequest);
        
        return updatedRequest;
    }
    
    @Transactional
    public SellRequest startNegotiation(Long requestId, BigDecimal offeredPrice, User adminUser) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        if (!sellRequest.isPending()) {
            throw new RuntimeException("Cannot negotiate with request with status: " + sellRequest.getStatus());
        }
        
        // Start negotiation
        sellRequest.startNegotiation(offeredPrice);
        sellRequest.setReviewedBy(adminUser);
        
        SellRequest updatedRequest = sellRequestRepository.save(sellRequest);
        
        // Notify the seller about negotiation offer
        sendNegotiationNotification(updatedRequest);
        
        return updatedRequest;
    }
    
    @Transactional
    public SellRequest completeSellRequest(Long requestId, User adminUser) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        if (!sellRequest.isApproved()) {
            throw new RuntimeException("Cannot complete request with status: " + sellRequest.getStatus());
        }
        
        // Mark as completed
        sellRequest.complete();
        
        SellRequest updatedRequest = sellRequestRepository.save(sellRequest);
        
        // Send completion notification
        sendCompletionNotification(updatedRequest);
        
        return updatedRequest;
    }
    
    @Transactional
    public SellRequest updateAdminNotes(Long requestId, String adminNotes, User adminUser) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        sellRequest.setAdminNotes(adminNotes);
        sellRequest.setReviewedBy(adminUser);
        sellRequest.setReviewedAt(LocalDateTime.now());
        
        return sellRequestRepository.save(sellRequest);
    }
    
    // ============== DASHBOARD & STATISTICS ==============
    
    @Transactional(readOnly = true)
    public long countByStatus(SellRequest.SellRequestStatus status) {
        return sellRequestRepository.countByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public long countPendingSellRequests() {
        return sellRequestRepository.countByStatus(SellRequest.SellRequestStatus.PENDING);
    }
    
    @Transactional(readOnly = true)
    public List<SellRequest> getRecentSellRequests(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "submittedAt"));
        return sellRequestRepository.findRecentRequests(pageable);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getSellRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        for (SellRequest.SellRequestStatus status : SellRequest.SellRequestStatus.values()) {
            stats.put(status.name().toLowerCase() + "Count", countByStatus(status));
        }
        
        // Financial statistics
        BigDecimal totalCompletedValue = sellRequestRepository.getTotalCompletedValue();
        Double averagePrice = sellRequestRepository.getAverageCompletedPrice();
        
        stats.put("totalCompletedValue", totalCompletedValue != null ? totalCompletedValue : BigDecimal.ZERO);
        stats.put("averageCompletedPrice", averagePrice != null ? averagePrice : 0.0);
        
        // Today's submissions
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        List<SellRequest> todayRequests = sellRequestRepository.findBetweenDates(startOfDay, endOfDay);
        stats.put("todaySubmissions", todayRequests.size());
        
        // Category distribution
        Map<String, Long> categoryDistribution = new HashMap<>();
        List<SellRequest> allRequests = sellRequestRepository.findAll();
        
        for (SellRequest request : allRequests) {
            String category = request.getItemCategory().name();
            categoryDistribution.put(category, categoryDistribution.getOrDefault(category, 0L) + 1);
        }
        
        stats.put("categoryDistribution", categoryDistribution);
        
        // Status distribution for charts
        Map<String, Long> statusDistribution = new HashMap<>();
        for (SellRequest.SellRequestStatus status : SellRequest.SellRequestStatus.values()) {
            statusDistribution.put(status.getDisplayName(), countByStatus(status));
        }
        stats.put("statusDistribution", statusDistribution);
        
        return stats;
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalValueByStatus(SellRequest.SellRequestStatus status) {
        return sellRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() == status && request.getFinalPrice() != null)
                .map(SellRequest::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // ============== USER-RELATED METHODS ==============
    
    @Transactional(readOnly = true)
    public List<SellRequest> getUserSellRequests(Long userId) {
        return sellRequestRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Page<SellRequest> getUserSellRequests(Long userId, Pageable pageable) {
        return sellRequestRepository.findByUserId(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<SellRequest> getUserSellRequestsByStatus(Long userId, SellRequest.SellRequestStatus status) {
        return sellRequestRepository.findByUserIdAndStatus(userId, status);
    }
    
    @Transactional
    public SellRequest cancelUserRequest(Long requestId, Long userId) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        // Check if user owns the request
        if (!sellRequest.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to cancel this request");
        }
        
        // Only allow cancellation of pending or negotiating requests
        if (!sellRequest.isPending() && !sellRequest.isNegotiating()) {
            throw new RuntimeException("Cannot cancel request with status: " + sellRequest.getStatus());
        }
        
        sellRequest.cancel();
        
        return sellRequestRepository.save(sellRequest);
    }
    
    @Transactional
    public SellRequest respondToNegotiation(Long requestId, Long userId, boolean accept, String notes) {
        SellRequest sellRequest = sellRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + requestId));
        
        // Check if user owns the request
        if (!sellRequest.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to respond to this negotiation");
        }
        
        if (!sellRequest.isNegotiating()) {
            throw new RuntimeException("This request is not in negotiation");
        }
        
        if (accept) {
            // Accept the negotiated price
            sellRequest.setFinalPrice(sellRequest.getNegotiatedPrice());
            sellRequest.setStatus(SellRequest.SellRequestStatus.APPROVED);
            sellRequest.setReviewedAt(LocalDateTime.now());
            
            // Create product from the accepted negotiation
            createProductFromSellRequest(sellRequest);
            
            sendNegotiationAcceptedNotification(sellRequest);
        } else {
            // Reject the negotiation, go back to pending
            sellRequest.setStatus(SellRequest.SellRequestStatus.PENDING);
            sellRequest.setNegotiatedPrice(null);
            sellRequest.setAdminNotes(notes);
            
            sendNegotiationRejectedNotification(sellRequest);
        }
        
        return sellRequestRepository.save(sellRequest);
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    @Transactional
    private void createProductFromSellRequest(SellRequest sellRequest) {
        try {
            // Validate required fields
            if (sellRequest.getFinalPrice() == null) {
                throw new IllegalArgumentException("Final price must be set before creating product");
            }
            
            if (sellRequest.getItemCondition() == null) {
                throw new IllegalArgumentException("Item condition is required");
            }
            
            // Create a new product from the approved sell request
            Product product = new Product();
            product.setName(sellRequest.getItemName());
            product.setDescription(
                sellRequest.getItemDescription() != null ? 
                sellRequest.getItemDescription() : 
                "Item from sell request #" + sellRequest.getId()
            );
            product.setCategory(sellRequest.getItemCategory());
            
            // This should work now that Product has getCondition() and setCondition() methods
            product.setCondition(sellRequest.getItemCondition());
            
            product.setPrice(sellRequest.getFinalPrice());
            product.setOriginalPrice(sellRequest.getExpectedPrice());
            product.setStockQuantity(1); // One item from the sell request
            product.setMinimumOrderQuantity(1);
            product.setSeller(sellRequest.getUser());
            product.setActive(true);
            product.setApproved(true);
            product.setAvailable(true);
            
            // Generate SKU from request ID
            String sku = "SR-" + String.format("%06d", sellRequest.getId()) + 
                        "-" + sellRequest.getItemCategory().name().substring(0, 3) +
                        "-" + sellRequest.getItemCondition().name().charAt(0);
            product.setSku(sku.toUpperCase());
            
            // Add tags based on category and condition
            List<String> tags = new ArrayList<>();
            tags.add("sell-request");
            tags.add("trade-in");
            tags.add("pre-owned");
            tags.add(sellRequest.getItemCategory().name().toLowerCase());
            
            // Add condition-specific tags
            switch(sellRequest.getItemCondition()) {
                case NEW:
                    tags.add("brand-new");
                    tags.add("unopened");
                    tags.add("sealed");
                    break;
                case REFURBISHED:
                    tags.add("refurbished");
                    tags.add("certified");
                    tags.add("renewed");
                    tags.add("like-new");
                    break;
                case USED_GOOD:
                    tags.add("used-good");
                    tags.add("good-condition");
                    tags.add("excellent-used");
                    break;
                case USED_FAIR:
                    tags.add("used-fair");
                    tags.add("fair-condition");
                    tags.add("working-condition");
                    break;
            }
            
            product.setTags(tags);
            
            // Set timestamps
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            
            // Save the product
            Product savedProduct = productRepository.save(product);
            
            // Update sell request with product reference
            sellRequest.setAdminNotes(
                (sellRequest.getAdminNotes() != null ? sellRequest.getAdminNotes() + "\n" : "") +
                "Converted to product ID: " + savedProduct.getId()
            );
            sellRequestRepository.save(sellRequest);
            
            System.out.println("✅ Product created from sell request #" + sellRequest.getId() + 
                             ": " + savedProduct.getName() + 
                             " (Condition: " + savedProduct.getCondition() + 
                             ", Price: " + savedProduct.getPrice() + 
                             ", SKU: " + savedProduct.getSku() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to create product from sell request #" + sellRequest.getId());
            e.printStackTrace();
            throw new RuntimeException("Failed to create product from sell request: " + e.getMessage(), e);
        }
    }
    
    // ============== NOTIFICATION METHODS ==============
    
    private void sendNewRequestNotification(SellRequest sellRequest) {
        System.out.println("📋 New sell request submitted: #" + sellRequest.getId() + 
                         " - " + sellRequest.getItemName() + 
                         " by " + sellRequest.getUser().getUsername() +
                         " for UGX " + sellRequest.getExpectedPrice());
        
        // In production, you would send email/SMS notification to admin here
    }
    
    private void sendApprovalNotification(SellRequest sellRequest) {
        System.out.println("✅ Sell request approved: #" + sellRequest.getId() + 
                         " - Final price: UGX " + sellRequest.getFinalPrice());
        
        // Notify seller via email/SMS
        /*
        if (sellRequest.getUser().getEmail() != null) {
            // Send email notification
        }
        */
    }
    
    private void sendRejectionNotification(SellRequest sellRequest) {
        System.out.println("❌ Sell request rejected: #" + sellRequest.getId() + 
                         " - Reason: " + sellRequest.getRejectionReason());
    }
    
    private void sendNegotiationNotification(SellRequest sellRequest) {
        System.out.println("🤝 Negotiation started for request: #" + sellRequest.getId() + 
                         " - Offered: UGX " + sellRequest.getNegotiatedPrice());
    }
    
    private void sendCompletionNotification(SellRequest sellRequest) {
        System.out.println("🎉 Sell request completed: #" + sellRequest.getId());
    }
    
    private void sendNegotiationAcceptedNotification(SellRequest sellRequest) {
        System.out.println("👍 Negotiation accepted for request: #" + sellRequest.getId());
    }
    
    private void sendNegotiationRejectedNotification(SellRequest sellRequest) {
        System.out.println("👎 Negotiation rejected for request: #" + sellRequest.getId());
    }
    
    // ============== SEARCH & FILTER METHODS ==============
    
    @Transactional(readOnly = true)
    public List<SellRequest> searchSellRequests(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSellRequests();
        }
        
        String searchTerm = keyword.toLowerCase().trim();
        List<SellRequest> allRequests = sellRequestRepository.findAll();
        
        return allRequests.stream()
                .filter(request -> 
                    request.getItemName().toLowerCase().contains(searchTerm) ||
                    (request.getItemDescription() != null && 
                     request.getItemDescription().toLowerCase().contains(searchTerm)) ||
                    request.getUser().getUsername().toLowerCase().contains(searchTerm) ||
                    (request.getRejectionReason() != null && 
                     request.getRejectionReason().toLowerCase().contains(searchTerm)) ||
                    (request.getAdminNotes() != null && 
                     request.getAdminNotes().toLowerCase().contains(searchTerm)))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SellRequest> filterSellRequests(SellRequest.SellRequestStatus status, 
                                               String category, 
                                               LocalDateTime startDate, 
                                               LocalDateTime endDate) {
        List<SellRequest> filteredRequests = sellRequestRepository.findAll();
        
        return filteredRequests.stream()
                .filter(request -> status == null || request.getStatus() == status)
                .filter(request -> category == null || 
                        request.getItemCategory().name().equalsIgnoreCase(category))
                .filter(request -> startDate == null || 
                        !request.getSubmittedAt().isBefore(startDate))
                .filter(request -> endDate == null || 
                        !request.getSubmittedAt().isAfter(endDate))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SellRequest> filterByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return sellRequestRepository.findAll().stream()
                .filter(request -> 
                    (minPrice == null || request.getExpectedPrice().compareTo(minPrice) >= 0) &&
                    (maxPrice == null || request.getExpectedPrice().compareTo(maxPrice) <= 0))
                .collect(Collectors.toList());
    }
    
    // ============== UTILITY METHODS ==============
    
    @Transactional(readOnly = true)
    public SellRequest getSellRequestById(Long id) {
        return sellRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sell request not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return sellRequestRepository.existsById(id);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (SellRequest.SellRequestStatus status : SellRequest.SellRequestStatus.values()) {
            counts.put(status.name(), countByStatus(status));
        }
        return counts;
    }
    
    @Transactional(readOnly = true)
    public Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        List<SellRequest> allRequests = sellRequestRepository.findAll();
        
        for (SellRequest request : allRequests) {
            String category = request.getItemCategory().name();
            counts.put(category, counts.getOrDefault(category, 0L) + 1);
        }
        
        return counts;
    }
    
    @Transactional
    public void bulkUpdateStatus(List<Long> requestIds, SellRequest.SellRequestStatus newStatus, User adminUser) {
        for (Long requestId : requestIds) {
            try {
                SellRequest request = getSellRequestById(requestId);
                request.setStatus(newStatus);
                request.setReviewedBy(adminUser);
                request.setReviewedAt(LocalDateTime.now());
                sellRequestRepository.save(request);
            } catch (Exception e) {
                System.err.println("Failed to update request #" + requestId + ": " + e.getMessage());
            }
        }
    }
    
    // ============== VALIDATION METHODS ==============
    
    public boolean validateSellRequest(SellRequestDTO requestDTO) {
        if (requestDTO.getItemName() == null || requestDTO.getItemName().trim().isEmpty()) {
            return false;
        }
        
        if (requestDTO.getExpectedPrice() == null || 
            requestDTO.getExpectedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (requestDTO.getItemCategory() == null) {
            return false;
        }
        
        if (requestDTO.getItemCondition() == null) {
            return false;
        }
        
        return true;
    }
    
    public List<String> validateSellRequestWithMessages(SellRequestDTO requestDTO) {
        List<String> errors = new ArrayList<>();
        
        if (requestDTO.getItemName() == null || requestDTO.getItemName().trim().isEmpty()) {
            errors.add("Item name is required");
        } else if (requestDTO.getItemName().length() > 200) {
            errors.add("Item name cannot exceed 200 characters");
        }
        
        if (requestDTO.getItemDescription() != null && 
            requestDTO.getItemDescription().length() > 2000) {
            errors.add("Description cannot exceed 2000 characters");
        }
        
        if (requestDTO.getExpectedPrice() == null) {
            errors.add("Expected price is required");
        } else if (requestDTO.getExpectedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be greater than 0");
        } else if (requestDTO.getExpectedPrice().compareTo(new BigDecimal("10000000")) > 0) {
            errors.add("Price cannot exceed 10,000,000");
        }
        
        if (requestDTO.getItemCategory() == null) {
            errors.add("Category is required");
        }
        
        if (requestDTO.getItemCondition() == null) {
            errors.add("Condition is required");
        }
        
        return errors;
    }
    
    
}