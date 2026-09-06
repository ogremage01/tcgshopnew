package com.shop.product.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shop.product.metadata.entity.Storage;
import java.util.List;
import java.util.Optional;
import com.shop.product.metadata.dto.StorageDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface StorageRepository extends JpaRepository<Storage, Long> {

    @Query("""
                SELECT new com.shop.product.metadata.dto.StorageDto(
                    s.id,
                    s.storageName,
                    s.description,
                    s.isDefault
                )
                FROM Storage s
                ORDER BY s.isDefault DESC, s.id ASC
            """)
    List<StorageDto> findAllByOrderByIsDefaultDescIdAsc();

    Optional<Storage> findById(Long id);

    @Modifying
    @Transactional
    @Query("""
                UPDATE Storage s
                SET s.isDefault = false
                WHERE s.isDefault = true
            """)
    void clearIsDefault();

}
