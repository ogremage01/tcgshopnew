package com.shop.search.dto.searching;

import java.util.List;

import com.shop.search.dto.enums.SearchEntryStateEnum;
import com.shop.search.dto.enums.SearchModeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchingDto {

    // 상품 검색 DTO

    // 상품 이름(한글 이름을 포함한다) - 검색용
    private String keyword;
    // 상품 게임(MTG/FAB 외 게임 라인) - 필터용 default: 전체 검색
    private List<String> games;
    // 상품 타입(Card/Sealed Product/supply?) - 필터용 default: 전체 검색
    private List<String> productTypes;
    // supplies 상품 타입 - 필터용 default: 전체 검색
    private List<String> suppliesTypes;
    /** 수동 상품 카테고리(preorder, event-ticket 등) — 필터용 */
    private List<String> manualCategories;
    // 카드 레어도 - 필터용 default: 전체 검색
    private List<String> rarities;
    // 카드 세트명 - 필터용 default: 전체 검색
    private List<String> setNames;
    /** UnionPrice.setCode — 세트 페이지 등 단일 세트 고정 시 */
    private String setCode;
    // 검색 UI 모드(default/advanced)
    private SearchModeEnum searchMode;
    // 화면 진입 상태(initial/updated)
    private SearchEntryStateEnum entryState;
    // 포일여부 - 필터용 default: 전체 검색/포일만/일반만 검색
    private Boolean isFoil;
    // 재고 유무 false: 재고 없는 것 포함, true: 재고 있는 것만 - 필터용 default: false
    private Boolean isInStock;

    public SearchModeEnum getResolvedSearchMode() {
        return searchMode == null ? SearchModeEnum.DEFAULT : searchMode;
    }

    public SearchEntryStateEnum getResolvedEntryState() {
        return entryState == null ? SearchEntryStateEnum.UPDATED : entryState;
    }

    public boolean isInitialEntry() {
        return getResolvedEntryState() == SearchEntryStateEnum.INITIAL;
    }

}
