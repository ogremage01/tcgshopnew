package com.shop.product.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shop.product.metadata.entity.SetNameMap;
import java.util.Optional;

public interface SetNameMapRepository extends JpaRepository<SetNameMap, Long> {

    Optional<SetNameMap> findByName(String name);

    Optional<SetNameMap> findByGameAndName(String game, String name);
}
