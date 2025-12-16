package com.IT_market.dto;

public class CartItemRequest {
    
    private String productId;
    private Integer quantity = 1;
    
    public CartItemRequest() {}
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}