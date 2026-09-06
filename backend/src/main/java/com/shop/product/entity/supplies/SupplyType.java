package com.shop.product.entity.supplies;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import com.shop.product.dto.supplies.SupplyTypeDto;

@Entity
@Table(name = "supply_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyType {
    // 서플라이 타입 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nameEn;
    @Column(nullable = false)
    private String nameKo;
    private Boolean isDeleted;

    public SupplyTypeDto toDto() {
        return SupplyTypeDto.builder()
                .id(id)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build();
    }
}
