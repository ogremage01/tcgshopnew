package com.shop.product.metadata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Table(name = "set_names")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPSetName {

    // TCGPlayer 세트 이름 엔티티(가격 동기화 시 세트 단위로 API 호출)
    // ex: IKO, MH3같은 세트 정보가 이곳에 저장된다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 세트 ID
    @Column(unique = true)
    private Long setNameId;
    // 카테고리 ID
    private Long categoryId;
    // 이름
    private String name;
    // 세트 이름 정리
    private String cleanSetName;
    // URL 경로명
    private String urlName;
    // 약어
    private String abbreviation;
    // 발매 일자
    private LocalDateTime releaseDate;
    // 미사용
    private Boolean isSupplemental;
    // 미사용
    private Boolean active;
}
