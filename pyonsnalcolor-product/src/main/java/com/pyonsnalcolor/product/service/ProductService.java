package com.pyonsnalcolor.product.service;

import com.pyonsnalcolor.product.model.BaseProduct;
import com.pyonsnalcolor.product.model.StoreType;
import com.pyonsnalcolor.product.repository.BasicProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@AllArgsConstructor
public class ProductService<T extends BaseProduct> {
    protected BasicProductRepository basicProductRepository;

    public Page<T> getProductsWithPaging(int pageNumber, int pageSize, String storeType) {
        switch (storeType.toUpperCase()) {
            case "CU":
            case "GS25":
            case "EMART24":
            case "SEVEN_ELEVEN":
                return basicProductRepository.findByStoreType(
                        Enum.valueOf(StoreType.class, storeType.toUpperCase()),
                        PageRequest.of(pageNumber, pageSize, Sort.by("updatedTime"))
                );
            default:
                return basicProductRepository.findAll(PageRequest.of(pageNumber, pageSize, Sort.by("updatedTime")));
        }
    }
}
