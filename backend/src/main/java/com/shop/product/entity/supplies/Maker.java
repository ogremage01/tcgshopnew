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
import com.shop.product.dto.supplies.MakerDto;
import jakarta.persistence.Column;

@Entity
@Table(name = "makers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Maker {

    // 제조사 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    // 삭제 여부
    private Boolean isDeleted;

    /**
     * 제조사 엔티티를 제조사 정보 Dto로 변환
     * 
     * @return 제조사 정보 Dto
     */
    public MakerDto toDto() {
        return MakerDto.builder()
                .id(id)
                .name(name)
                .build();
    }

}
