package com.beautyshop.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "danh_muc")
@AttributeOverride(name = "id", column = @Column(name = "ma_danh_muc"))
@AttributeOverride(name = "name", column = @Column(name = "ten_danh_muc", nullable = false, length = 160))
public class Category extends CatalogItem {
    protected Category() {}
}
