package com.beautyshop.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CatalogForm {
    @NotBlank(message = "Vui lòng nhập tên.")
    @Size(max = 255, message = "Tên tối đa 255 ký tự.")
    private String name;

    @NotBlank(message = "Vui lòng nhập đường dẫn.")
    @Size(max = 190, message = "Đường dẫn tối đa 190 ký tự.")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Dùng chữ thường không dấu, số và dấu gạch ngang.")
    private String slug;

    private boolean active = true;

    private Long categoryId;

    private Long brandId;

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    private String description;

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    private String ingredients;

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    private String usageGuide;

    @Size(max = 600, message = "Tối đa 600 ký tự.")
    private String skinTypeTags;

    static CatalogForm from(CatalogItem item) {
        CatalogForm form = new CatalogForm();
        form.name = item.getName();
        form.slug = item.getSlug();
        form.active = item.isActive();
        if (item instanceof Product product) {
            form.categoryId = product.getCategory().getId();
            form.brandId = product.getBrand().getId();
            form.description = product.getDescription();
            form.ingredients = product.getIngredients();
            form.usageGuide = product.getUsageGuide();
            form.skinTypeTags = product.getSkinTypeTags();
        }
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getUsageGuide() { return usageGuide; }
    public void setUsageGuide(String usageGuide) { this.usageGuide = usageGuide; }

    public String getSkinTypeTags() { return skinTypeTags; }
    public void setSkinTypeTags(String skinTypeTags) { this.skinTypeTags = skinTypeTags; }
}
