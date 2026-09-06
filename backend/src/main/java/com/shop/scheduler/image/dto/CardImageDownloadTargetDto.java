package com.shop.scheduler.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardImageDownloadTargetDto {

    // 이미지 다운로드 대상 제품 정보 Dto

    // 제품 고유 ID
    private Long productId;
    // 소속 게임
    private String game;
    // 세트 약어
    private String setAbbrv;

}
