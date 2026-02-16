package com.example.pricewatch.service;

import com.example.pricewatch.domain.model.Product;
import com.example.pricewatch.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    //등록 , 가져오기 , 삭제

    private final ProductRepository productRepository;


}
  