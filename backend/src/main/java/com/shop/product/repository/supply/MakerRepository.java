package com.shop.product.repository.supply;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shop.product.entity.supplies.Maker;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface MakerRepository extends JpaRepository<Maker, Long> {

    @Query("SELECT m FROM Maker m WHERE m.isDeleted = :isDeleted ORDER BY m.name ASC")
    List<Maker> findAllByIsDeleted(Boolean isDeleted);

    @Query("SELECT m FROM Maker m WHERE m.isDeleted = :isDeleted")
    Page<Maker> findAllByIsDeleted(Boolean isDeleted, Pageable pageable);

    List<Maker> findAll();

    Optional<Maker> findByName(String name);

}
