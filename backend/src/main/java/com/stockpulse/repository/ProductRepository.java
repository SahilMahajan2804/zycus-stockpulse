package com.stockpulse.repository;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(Category category);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByStatusAndCategory(ProductStatus status, Category category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") String productId);

    @Query("select avg(p.demandVelocity) from Product p where p.category = :category and p.id <> :productId")
    Double averagePeerVelocity(@Param("category") Category category, @Param("productId") String productId);
}
