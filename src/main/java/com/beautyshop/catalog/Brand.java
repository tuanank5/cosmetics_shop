package com.beautyshop.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "thuong_hieu")
@AttributeOverride(name = "id", column = @Column(name = "ma_thuong_hieu"))
@AttributeOverride(name = "name", column = @Column(name = "ten_thuong_hieu", nullable = false, length = 160))
public class Brand extends CatalogItem {
    protected Brand() {}
}
