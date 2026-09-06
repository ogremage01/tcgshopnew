package com.shop.common.page.dto;

import java.util.List;

import org.springframework.data.domain.Sort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageParam {

    /* 페이지 번호 (0-based) */
    private Integer page;
    /* 페이지 크기 */
    private Integer size;
    /**
     * Spring 페이지 요청의 {@code sort}와 같은 규칙: {@code "속성명"}, 또는 {@code "속성명,asc|desc"}.
     * 다중 정렬은 항목 순서대로 {@link Sort#and(Sort)} 결합.
     */
    private List<String> sort;

    /**
     * {@link #sort}를 {@link org.springframework.data.domain.Pageable}에 넣을 수 있는 {@link Sort}로 변환.
     */
    public Sort toSort() {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }
        return sort.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(PageParam::parseSortSpec)
                .reduce(Sort::and)
                .orElse(Sort.unsorted());
    }

    private static Sort parseSortSpec(String raw) {
        if (raw == null || raw.isBlank()) {
            return Sort.unsorted();
        }
        String spec = raw.trim();
        int comma = spec.lastIndexOf(',');
        if (comma < 0) {
            return Sort.by(spec);
        }
        String property = spec.substring(0, comma).trim();
        String direction = spec.substring(comma + 1).trim();
        return Sort.by(Sort.Direction.fromString(direction), property);
    }
}
