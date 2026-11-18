package com.kampotourism.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category_translation")
public class CategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String lang;

    @Column(nullable = false)
    private String name;

    private String description;

    protected CategoryTranslation() {
    }

    CategoryTranslation(Long categoryId, String lang) {
        this.categoryId = categoryId;
        this.lang = lang;
    }

    void edit(String name, String description) {
        this.name = name.trim();
        this.description = (description == null || description.isBlank()) ? null : description.trim();
    }

    public Long getCategoryId() { return categoryId; }
    public String getLang() { return lang; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
