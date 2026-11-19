package com.beautyshop.catalog;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
class CatalogService {
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;

    CatalogService(CategoryRepository categories, BrandRepository brands, ProductRepository products) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<? extends CatalogItem> list(CatalogSection section) {
        return repository(section).findAll(Sort.by("name", "id"));
    }

    @Transactional(readOnly = true)
    public CatalogForm edit(CatalogSection section, long id) {
        return CatalogForm.from(find(repository(section), id));
    }

    @Transactional
    public void save(CatalogSection section, Long id, CatalogForm form, Errors errors) {
        switch (section) {
            case CATEGORIES -> saveTaxonomy(categories, id, new Category(), form, errors);
            case BRANDS -> saveTaxonomy(brands, id, new Brand(), form, errors);
            case PRODUCTS -> saveProduct(id, form, errors);
        }
    }

    private <T extends CatalogItem> void saveTaxonomy(
            CatalogRepository<T> repository, Long id, T newItem, CatalogForm form, Errors errors) {
        if (form.getName().strip().length() > 160) {
            errors.rejectValue("name", "catalog.name.length", "Tên tối đa 160 ký tự.");
            return;
        }
        T item = id == null ? newItem : find(repository, id);
        item.update(form);
        repository.saveAndFlush(item);
    }

    private void saveProduct(Long id, CatalogForm form, Errors errors) {
        Category category = select(categories, form.getCategoryId(), "categoryId", errors);
        Brand brand = select(brands, form.getBrandId(), "brandId", errors);
        if (errors.hasErrors()) {
            return;
        }
        Product product = id == null ? new Product() : find(products, id);
        product.update(form, category, brand);
        products.saveAndFlush(product);
    }

    private <T extends CatalogItem> T select(
            CatalogRepository<T> repository, Long id, String field, Errors errors) {
        T item = id == null ? null : repository.findById(id).orElse(null);
        if (item == null) {
            errors.rejectValue(field, "catalog.selection", "Vui lòng chọn một mục hợp lệ.");
        }
        return item;
    }

    @Transactional
    public void remove(CatalogSection section, long id) {
        if (section == CatalogSection.PRODUCTS) {
            find(products, id).deactivate();
            return;
        }
        delete(repository(section), id);
    }

    private <T extends CatalogItem> void delete(CatalogRepository<T> repository, long id) {
        repository.delete(find(repository, id));
        repository.flush();
    }

    private CatalogRepository<? extends CatalogItem> repository(CatalogSection section) {
        return switch (section) {
            case CATEGORIES -> categories;
            case BRANDS -> brands;
            case PRODUCTS -> products;
        };
    }

    private <T extends CatalogItem> T find(CatalogRepository<T> repository, long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
