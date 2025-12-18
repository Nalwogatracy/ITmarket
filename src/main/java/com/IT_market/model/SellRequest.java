
package com.IT_market.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sell_requests")
public class SellRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    
    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_category", nullable = false)
    private Product.ProductCategory itemCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private Product.ProductCondition itemCondition;
    
    @Column(name = "expected_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedPrice;
    
    @Column(name = "negotiated_price", precision = 10, scale = 2)
    private BigDecimal negotiatedPrice;
    
    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellRequestStatus status = SellRequestStatus.PENDING;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    // Status Enum
    public enum SellRequestStatus {
        PENDING("Pending Review"),
        UNDER_REVIEW("Under Review"),
        NEGOTIATING("Price Negotiation"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        SellRequestStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructors
    public SellRequest() {
        this.submittedAt = LocalDateTime.now();
    }
    
    public SellRequest(User user, String itemName, String itemDescription, 
                      Product.ProductCategory itemCategory, Product.ProductCondition itemCondition,
                      BigDecimal expectedPrice) {
        this();
        this.user = user;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory;
        this.itemCondition = itemCondition;
        this.expectedPrice = expectedPrice;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    
    public Product.ProductCategory getItemCategory() { return itemCategory; }
    public void setItemCategory(Product.ProductCategory itemCategory) { this.itemCategory = itemCategory; }
    
    public Product.ProductCondition getItemCondition() { return itemCondition; }
    public void setItemCondition(Product.ProductCondition itemCondition) { this.itemCondition = itemCondition; }
    
    public BigDecimal getExpectedPrice() { return expectedPrice; }
    public void setExpectedPrice(BigDecimal expectedPrice) { this.expectedPrice = expectedPrice; }
    
    public BigDecimal getNegotiatedPrice() { return negotiatedPrice; }
    public void setNegotiatedPrice(BigDecimal negotiatedPrice) { this.negotiatedPrice = negotiatedPrice; }
    
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    
    public SellRequestStatus getStatus() { return status; }
    public void setStatus(SellRequestStatus status) { this.status = status; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    
    // Helper methods
    public boolean isPending() {
        return status == SellRequestStatus.PENDING;
    }
    
    public boolean isNegotiating() {
        return status == SellRequestStatus.NEGOTIATING;
    }
    
    public boolean isApproved() {
        return status == SellRequestStatus.APPROVED;
    }
    
    public boolean isCompleted() {
        return status == SellRequestStatus.COMPLETED;
    }
    
    public String getStatusColor() {
        switch (status) {
            case PENDING: return "warning";
            case UNDER_REVIEW: return "info";
            case NEGOTIATING: return "primary";
            case APPROVED: return "success";
            case REJECTED: return "danger";
            case COMPLETED: return "secondary";
            case CANCELLED: return "dark";
            default: return "light";
        }
    }
    
    // Business logic
    public void startNegotiation(BigDecimal offeredPrice) {
        this.negotiatedPrice = offeredPrice;
        this.status = SellRequestStatus.NEGOTIATING;
        this.reviewedAt = LocalDateTime.now();
    }
    
    public void approve(BigDecimal finalPrice, User reviewedBy) {
        this.finalPrice = finalPrice;
        this.status = SellRequestStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = LocalDateTime.now();
    }
    
    public void reject(String reason, User reviewedBy) {
        this.rejectionReason = reason;
        this.status = SellRequestStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = LocalDateTime.now();
    }
    
    public void complete() {
        this.status = SellRequestStatus.COMPLETED;
    }
    
    public void cancel() {
        this.status = SellRequestStatus.CANCELLED;
        this.reviewedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        if (status != SellRequestStatus.PENDING && reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }
}