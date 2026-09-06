package com.shop.admin.product.service.metadata;

import com.shop.admin.product.dto.metadata.ProductConfigBootstrapDto;

public interface AdminProductConfigBootstrapService {

    /**
     * 상품 설정 화면 초기 데이터 (동기 목록·최신 동기 시각·저장소·가격 정책)을 한 번에 조회합니다.
     */
    ProductConfigBootstrapDto getBootstrap();
}
