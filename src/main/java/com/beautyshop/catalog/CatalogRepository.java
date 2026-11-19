package com.beautyshop.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CatalogRepository<T extends CatalogItem> extends JpaRepository<T, Long> {
}
