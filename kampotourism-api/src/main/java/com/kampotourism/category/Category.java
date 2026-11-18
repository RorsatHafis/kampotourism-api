package com.kampotourism.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CategoryKind kind;

    @Column(nullable = false)
    private String slug;

    private int sortOrder;

    @Version
    private long version;

    protected Category() {
    }

    Category(CategoryKind kind, String slug, int sortOrder) {
        this.kind = kind;
        this.slug = slug;
        this.sortOrder = sortOrder;
    }

    void edit(String slug, int sortOrder) {
        this.slug = slug;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public CategoryKind getKind() { return kind; }
    public String getSlug() { return slug; }
    public int getSortOrder() { return sortOrder; }
    public long getVersion() { return version; }
}
