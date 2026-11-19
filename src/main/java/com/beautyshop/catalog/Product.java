package com.beautyshop.catalog;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@AttributeOverride(name = "id", column = @Column(name = "ma_san_pham"))
@AttributeOverride(name = "name", column = @Column(name = "ten_san_pham", nullable = false, length = 255))
public class Product extends CatalogItem {
    protected Product() {}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_danh_muc", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_thuong_hieu", nullable = false)
    private Brand brand;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thanh_phan", columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "huong_dan_su_dung", columnDefinition = "TEXT")
    private String usageGuide;

    @Column(name = "loai_da_phu_hop", length = 600)
    private String skinTypeTags;

    @Column(name = "ngay_tao", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    void update(CatalogForm form, Category category, Brand brand) {
        super.update(form);
        this.category = category;
        this.brand = brand;
        description = form.getDescription();
        ingredients = form.getIngredients();
        usageGuide = form.getUsageGuide();
        skinTypeTags = form.getSkinTypeTags();
    }

    public Category getCategory() { return category; }
    public Brand getBrand() { return brand; }
    public String getDescription() { return description; }
    public String getIngredients() { return ingredients; }
    public String getUsageGuide() { return usageGuide; }
    public String getSkinTypeTags() { return skinTypeTags; }
}
