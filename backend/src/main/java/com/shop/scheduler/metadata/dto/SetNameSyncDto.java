package com.shop.scheduler.metadata.dto;

import com.shop.product.metadata.entity.TcgPSetName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SetNameSyncDto {

    // 세트 이름 동기화 정보 Dto

    private Long setNameId;
    private String name;
    private String cleanSetName;
    private String urlName;
    private String abbreviation;
    private LocalDateTime releaseDate;
    private Boolean isSupplemental;
    private Boolean active;

    /**
     * 세트 이름 엔티티로 변환한다.
     * 
     * @param id         세트 이름 ID
     * @param categoryId 카테고리 ID
     * @return 세트 이름 엔티티
     */
    public TcgPSetName toEntity(Long id, Long categoryId) {
        return TcgPSetName.builder()
                .id(id)
                .setNameId(setNameId)
                .categoryId(categoryId)
                .name(name)
                .cleanSetName(cleanSetName)
                .urlName(urlName)
                .abbreviation(abbreviation)
                .releaseDate(releaseDate)
                .isSupplemental(isSupplemental)
                .active(active)
                .build();
    }
}
