package com.beautyshop.catalog;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class CatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "duong_dan", nullable = false, length = 190)
    private String slug;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean active;

    void update(CatalogForm form) {
        name = form.getName().strip();
        slug = form.getSlug();
        active = form.isActive();
    }

    void deactivate() {
        active = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public boolean isActive() { return active; }
}
