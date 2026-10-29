package com.shopping.model;

import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private Integer inventory;

    public Product() {}

    public Product(String name, String description, Double price, String imageUrl, Integer inventory) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.inventory = inventory;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getInventory() { return inventory; }
    public void setInventory(Integer inventory) { this.inventory = inventory; }
}
