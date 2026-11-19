package com.beautyshop.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@WithMockUser(roles = "ADMIN")
class CatalogMariaDbTest {
    private static final String ADMIN = "/admin/catalog/";
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;

    @Test
    void catalogCrudUsesV1ConstraintsAndPreservesProducts() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String categorySlug = "category-" + suffix;
        String brandSlug = "brand-" + suffix;
        String productSlug = "product-" + suffix;
        mvc.perform(post(ADMIN + "categories/new").with(csrf())
                .param("name", "Chăm sóc da").param("slug", categorySlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "categories"));
        mvc.perform(post(ADMIN + "brands/new").with(csrf())
                .param("name", "Thương hiệu kiểm thử").param("slug", brandSlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "brands"));
        long category = idFor(categories, categorySlug);
        long brand = idFor(brands, brandSlug);

        mvc.perform(post(ADMIN + "categories/" + category + "/edit").with(csrf())
                .param("name", "Danh mục đã cập nhật").param("slug", categorySlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "categories"));
        mvc.perform(post(ADMIN + "brands/" + brand + "/edit").with(csrf())
                .param("name", "Thương hiệu đã cập nhật").param("slug", brandSlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "brands"));
        assertThat(categories.findById(category).orElseThrow().getName()).isEqualTo("Danh mục đã cập nhật");
        assertThat(brands.findById(brand).orElseThrow().getName()).isEqualTo("Thương hiệu đã cập nhật");

        mvc.perform(post(ADMIN + "products/new").with(csrf())
                .param("name", "Sản phẩm kiểm thử").param("slug", productSlug))
            .andExpect(model().attributeHasFieldErrors("form", "categoryId", "brandId"));
        mvc.perform(post(ADMIN + "products/new").with(csrf())
                .param("name", "Sản phẩm kiểm thử").param("slug", productSlug)
                .param("categoryId", Long.toString(category)).param("brandId", Long.toString(brand))
                .param("ingredients", "Thành phần kiểm thử").param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "products"));
        long product = idFor(products, productSlug);
        assertThat(products.findById(product).orElseThrow().getIngredients()).isEqualTo("Thành phần kiểm thử");

        mvc.perform(get(ADMIN + "products"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(productSlug)));
        mvc.perform(get(ADMIN + "products/" + product + "/edit"))
            .andExpect(status().isOk());
        mvc.perform(post(ADMIN + "products/" + product + "/edit").with(csrf())
                .param("name", "Tên đã cập nhật").param("slug", productSlug)
                .param("categoryId", Long.toString(category)).param("brandId", Long.toString(brand))
                .param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "products"));
        assertThat(products.findById(product).orElseThrow().getName()).isEqualTo("Tên đã cập nhật");

        mvc.perform(post(ADMIN + "categories/new").with(csrf())
                .param("name", "Tên trùng đường dẫn").param("slug", categorySlug))
            .andExpect(model().attributeHasErrors("form"));
        mvc.perform(post(ADMIN + "categories/" + category + "/delete").with(csrf()))
            .andExpect(flash().attributeExists("error"));
        mvc.perform(post(ADMIN + "brands/" + brand + "/delete").with(csrf()))
            .andExpect(flash().attributeExists("error"));
        assertThat(categories.existsById(category)).isTrue();
        assertThat(brands.existsById(brand)).isTrue();

        mvc.perform(post(ADMIN + "products/" + product + "/delete").with(csrf()))
            .andExpect(redirectedUrl(ADMIN + "products"));
        assertThat(products.findById(product).orElseThrow().isActive()).isFalse();

        String unusedSlug = "unused-" + suffix;
        mvc.perform(post(ADMIN + "brands/new").with(csrf())
                .param("name", "Thương hiệu chưa dùng").param("slug", unusedSlug))
            .andExpect(redirectedUrl(ADMIN + "brands"));
        long unusedBrand = idFor(brands, unusedSlug);
        mvc.perform(post(ADMIN + "brands/" + unusedBrand + "/delete").with(csrf()))
            .andExpect(flash().attributeExists("message"));
        assertThat(brands.existsById(unusedBrand)).isFalse();
    }

    private long idFor(CatalogRepository<?> repository, String slug) {
        return repository.findAll().stream()
            .filter(item -> item.getSlug().equals(slug))
            .findFirst().orElseThrow().getId();
    }
}
