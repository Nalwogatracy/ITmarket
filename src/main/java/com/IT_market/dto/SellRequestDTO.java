// File: src/main/java/com/IT_market/dto/SellRequestDTO.java
package com.IT_market.dto;

import com.IT_market.model.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class SellRequestDTO {
    
    @NotBlank(message = "Item name is required")
    @Size(max = 200, message = "Item name cannot exceed 200 characters")
    private String itemName;
    
    @NotBlank(message = "Account type is required")
    private String accountType;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String itemDescription;
    
    @NotNull(message = "Category is required")
    private Product.ProductCategory itemCategory;
    
    @NotNull(message = "Condition is required")
    private Product.ProductCondition itemCondition;
    
    @NotNull(message = "Expected price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "10000000", message = "Price cannot exceed 10,000,000")
    private BigDecimal expectedPrice;
    
    private String additionalNotes;
    
    // Getters and Setters
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
    
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
}