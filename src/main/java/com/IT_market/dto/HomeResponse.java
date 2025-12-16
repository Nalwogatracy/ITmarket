package com.IT_market.dto;

import com.IT_market.model.Product;
import java.util.List;

public class HomeResponse {

    private List<Product> featuredProducts;
    private List<Product> latestProducts;
    private List<String> categories;

    public List<Product> getFeaturedProducts() {
        return featuredProducts;
    }

    public void setFeaturedProducts(List<Product> featuredProducts) {
        this.featuredProducts = featuredProducts;
    }

    public List<Product> getLatestProducts() {
        return latestProducts;
    }

    public void setLatestProducts(List<Product> latestProducts) {
        this.latestProducts = latestProducts;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    
}
