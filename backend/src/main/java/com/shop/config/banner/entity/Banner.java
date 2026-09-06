package com.shop.config.banner.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.shop.config.banner.dto.BannerDto;
@Entity
@Table(name = "banners")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    // 배너 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 배치 페이지
    private String target;
    // 배너 이미지 URL
    private String imageUrl;
    // 배너 링크
    private String link;
    // 배너 제목
    private String title;
    // 배너 표시 순서
    private Integer displayOrder;
    // 배너 활성 여부
    private Boolean active;
    // 생성일시
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BannerDto toDto() {
        return BannerDto.builder()
            .id(id)
            .target(target)
            .imageUrl(imageUrl)
            .link(link)
            .title(title)
            .displayOrder(displayOrder)
            .build();
    }
}
