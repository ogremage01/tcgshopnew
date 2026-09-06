package com.shop.admin.analyze.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminWeeklyReportDto extends AdminDailyReportDto {

    /**
     * 온라인 매출 세부 정보(싱글카드/밀봉/서플라이/수동상품)
     */
    private OnlineSalesSummaryDto onlineSalesSummaryDto;
    /**
     * 오프라인 매출 세부 정보(밀봉/서플라이/수동상품)
     */
    private OfflineSalesSummaryDto offlineSalesSummaryDto;

    /**
     * 프론트 그래프용 기간별 매출(주간 보고: 일별, 월간 보고: 해당 월 일별).
     */
    private List<AdminDailySalesReportRowDto> dailySalesReportRowDtoList;

    //온라인 매출 세부 정보 컨테이너
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class OnlineSalesSummaryDto {
        private List<SingleCardSalesSummaryDto> singleCardSalesSummaryList;
        private List<SealedProductSalesSummaryDto> sealedProductSalesSummaryList;
        private SupplyProductSalesSummaryDto supplyProductSalesSummaryDto;
        private ManualProductSalesSummaryDto manualProductSalesSummaryDto;
    }

    //온라인-싱글카드 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SingleCardSalesSummaryDto {
        /**
         * 게임 이름(select distinct game from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "CARD_PRODUCT" group by PS.game)
         */
        private String game;
        private List<CardProductSaleDto> cardProductSaleDtoList;
    }

    //온라인-싱글카드 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class CardProductSaleDto {
        /**
         * 카드 세트 이름(select distinct setName from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "CARD_PRODUCT" group by PS.setName)
         * 판매 순위 1,2,3위까지만 개별 항목으로, 나머지는 etc(기타)로 처리
         */
        private String setName;
        private Long totalSalesCount;
        private Long totalSalesQuantity;
        private Long totalSalesAmount;
        private Double totalSalesPercentage;
    }

    //온라인-밀봉제품 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SealedProductSalesSummaryDto {
        /**
         * 게임 이름(select distinct game from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "SEALED_PRODUCT" group by PS.game)
         */
        private String game;
        private List<SealedProductSaleDto> sealedProductSaleDtoList;
    }

    //온라인-밀봉제품 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SealedProductSaleDto {
        /**
         * 밀봉제품 세트 이름(select distinct setName from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "SEALED_PRODUCT" group by PS.setName)
         * 판매 순위 순으로 나열. 전체를 보여준다.
         */
        private String setName;
        private Long totalSalesCount;
        private Long totalSalesQuantity;
        private Long totalSalesAmount;
        private Double totalSalesPercentage;
    }

    //온라인 서플라이 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SupplyProductSalesSummaryDto {
        private List<SupplyProductSaleDto> supplyProductSaleDtoList;
    }
    //온라인 서플라이 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class SupplyProductSaleDto {
        /**
         * 서플라이 판매 상세 정보(select distinct setName from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "SUPPLY" group by PS.setName)
         * 판매 순위 순으로 나열. 전체를 보여준다.
         */
        private String supplyType;
        private Long totalSalesCount;
        private Long totalSalesQuantity;
        private Long totalSalesAmount;
        private Double totalSalesPercentage;
    }

    //온라인 수동상품 판매 요약 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class ManualProductSalesSummaryDto {
        private List<ManualProductSaleDto> manualProductSaleDtoList;
    }
    //온라인 수동상품 판매 상세 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class ManualProductSaleDto {

        /**
         * 수동상품 판매 상세 정보(select distinct manualType from orderProduct as OP left join productsearchMap as PS on OP.searchMapId = PS.id where productTable = "MANUAL_PRODUCT" group by PS.manualType)
         */
        private String manualType;
        private Long totalSalesCount;
        private Long totalSalesQuantity;
        private Long totalSalesAmount;
        private Double totalSalesPercentage;
    }


     /**
     * 오프라인 판매 상품과 온라인 제품 연결(select OP.linkTable, OP.linkId from offlineSalesItem as OSI Join offlineProduct as OP on OSI.title = OP.title)
     * 이 정보가 각기 연결되어 있다
     * 오프라인에서 판매된 상품(offlineSalesItem)과 온라인에 등록된 오프라인 상품 정보(offlineProduct)가 연결되어 있다.
     * 그리고 이 정보를 기반으로 연결된 온라인 제품 정보를 조회할 수 있다.
     * 해당 정보(linkTable linkId)를 기반으로 카테고리화 하여 오프라인 판매 정보를 집계한다.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class OfflineSalesSummaryDto {

        /**
         * linkTable: Sealed (온라인과 동일하게 게임별로 분할)
         */
        private List<SealedProductSalesSummaryDto> sealedProductSalesSummaryList;
        /**
         * linkTable: Supply
         */
        private List<SupplyProductSaleDto> supplyProductSaleDtoList;
        /**
         * linkTable: Manual
         */
        private List<ManualProductSaleDto> manualProductSaleDtoList;
    }

}
