package com.shop.offline.product.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.offline.product.dto.OfflineCatalogResponse;
import com.shop.offline.product.dto.OfflineProductDto;
import com.shop.offline.product.dto.OfflineProductReceivingHistoryDto;
import com.shop.offline.product.dto.OfflineProductReceivingRequest;
import com.shop.offline.product.dto.OfflineShippingReconcileResultDto;
import com.shop.offline.product.dto.OfflineStockSyncResultDto;
import com.shop.offline.product.dto.ProductRawDto;
import com.shop.offline.product.dto.SimpleRegisterOfflineProductDto;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.entity.OfflineProductReceivingHistory;
import com.shop.offline.product.entity.OfflineProductReceivingItem;
import com.shop.offline.product.repository.OfflineProductReceivingHistoryRepository;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.common.excel.service.ExcelService;
import com.shop.common.identity.AuthenticatedUserProvider;
import com.shop.product.service.manualProduct.ManualProductService;
import com.shop.product.service.sealedProduct.SealedProductService;
import com.shop.product.service.supply.AdminSupplyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineProductServiceImpl implements OfflineProductService {

    private static final String TOSS_OPEN_API_URL = "https://open-api.tossplace.com/api-public/openapi/v1/merchants/{merchantId}/";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 500;
    private static final int WEBCLIENT_MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id",
            "productId",
            "categoryTitle",
            "title",
            "priceValue",
            "receivingQuantity",
            "shippingQuantity",
            "stockQuantity",
            "createdAt",
            "updatedAt",
            "linkTableName");

    private final WebClient webClient = WebClient.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs()
                    .maxInMemorySize(WEBCLIENT_MAX_IN_MEMORY_BYTES))
            .build();
    private final ObjectMapper objectMapper;
    private final OfflineProductRepository offlineProductRepository;
    private final OfflineProductReceivingHistoryRepository receivingHistoryRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final SealedProductService sealedProductService;
    private final ManualProductService manualProductService;
    private final AdminSupplyService adminSupplyService;
    private final OfflineReceivingOnlineStockAdjuster receivingOnlineStockAdjuster;
    private final OfflineShippingQuantityAdjuster shippingQuantityAdjuster;
    private final ExcelService excelService;

    @Value("${app.toss-place.access-key}")
    private String accessKey;
    @Value("${app.toss-place.secret-key}")
    private String secretKey;
    @Value("${app.toss-place.merchant-id}")
    private String merchantId;

    @Override
    @Transactional
    public void downloadOfflineProducts() {
        int page = 1;
        while (fetchAndSaveOfflineProducts(page)) {
            page++;
        }
    }

    private boolean fetchAndSaveOfflineProducts(int page) {
        String url = buildTossApiUrl("catalog/items?page=" + page + "&size=" + PAGE_SIZE);
        String response = webClient.get()
            .uri(url)
            .header("X-access-key", accessKey)
            .header("X-secret-key", secretKey)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        return saveOfflineProducts(response);
    }

    @Override
    public void downloadOfflineProductCategories() {
        String url = buildTossApiUrl("catalog/categories?page=1&size=" + PAGE_SIZE);
        webClient.get()
            .uri(url)
            .header("X-access-key", accessKey)
            .header("X-secret-key", secretKey)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelFileResult downloadOfflineProductsExcel() {
        List<OfflineProductDto> products = offlineProductRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return excelService.generateOfflineProductsExcel(products);
    }

    private String buildTossApiUrl(String path) {
        return TOSS_OPEN_API_URL.replace("{merchantId}", merchantId) + path;
    }

    @Transactional
    private boolean saveOfflineProducts(String response) {
        try {
            OfflineCatalogResponse catalogResponse = objectMapper.readValue(response, OfflineCatalogResponse.class);
            if (!"SUCCESS".equals(catalogResponse.getResultType())) {
                log.warn("오프라인 상품 API 응답 실패: resultType={}", catalogResponse.getResultType());
                return false;
            }
            List<ProductRawDto> productRawDtos = catalogResponse.getSuccess() != null
                ? catalogResponse.getSuccess()
                : List.of();
            if (productRawDtos.isEmpty()) {
                return false;
            }
            for (ProductRawDto dto : productRawDtos) {
                if (dto.getId() == null || dto.getCategory() == null || dto.getPrice() == null) {
                    log.warn("오프라인 상품 필수 필드 누락: productId={}", dto.getId());
                    continue;
                }
                OfflineProduct entity = offlineProductRepository.findByProductId(dto.getId())
                    .map(existing -> updateFields(existing, dto))
                    .orElseGet(() -> toNewEntity(dto));
                offlineProductRepository.save(entity);
            }
            return true;
        } catch (JsonProcessingException e) {
            log.error("오프라인 상품 응답 파싱 실패: {}", e.getMessage());
            return false;
        }
    }

    private OfflineProduct toNewEntity(ProductRawDto dto) {
        return OfflineProduct.builder()
            .productId(dto.getId())
            .categoryId(dto.getCategory().getId())
            .categoryTitle(dto.getCategory().getTitle())
            .title(dto.getTitle())
            .priceUnit(dto.getPrice().getPriceUnit())
            .priceValue(dto.getPrice().getPriceValue())
            .barcode(dto.getPrice().getBarcode())
            .createdAt(toLocalDateTime(dto.getCreatedAt()))
            .updatedAt(toLocalDateTime(dto.getUpdatedAt()))
            .build();
    }

    private OfflineProduct updateFields(OfflineProduct existing, ProductRawDto dto) {
        existing.setCategoryId(dto.getCategory().getId());
        existing.setCategoryTitle(dto.getCategory().getTitle());
        existing.setTitle(dto.getTitle());
        existing.setPriceUnit(dto.getPrice().getPriceUnit());
        existing.setPriceValue(dto.getPrice().getPriceValue());
        existing.setBarcode(dto.getPrice().getBarcode());
        existing.setCreatedAt(toLocalDateTime(dto.getCreatedAt()));
        existing.setUpdatedAt(toLocalDateTime(dto.getUpdatedAt()));
        return existing;
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, KST) : null;
    }

    @Override
    public Page<OfflineProductDto> getOfflineProductList(
            String sortBy,
            String sortDirection,
            Pageable pageable) {
        validateSort(sortBy, sortDirection);
        return offlineProductRepository.findSorted(null, sortBy, sortDirection, pageable).map(this::toDto);
    }

    @Override
    public Page<OfflineProductDto> getOfflineProductListByKeyword(
            String keyword,
            String sortBy,
            String sortDirection,
            Pageable pageable) {
        validateSort(sortBy, sortDirection);
        return offlineProductRepository.findSorted(keyword, sortBy, sortDirection, pageable).map(this::toDto);
    }

    private void validateSort(String sortBy, String sortDirection) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 기준입니다.");
        }
        if (!"asc".equals(sortDirection) && !"desc".equals(sortDirection)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정렬 방향은 asc 또는 desc여야 합니다.");
        }
    }

    @Override
    public OfflineProductDto getOfflineProductByProductId(String productId) {
        return offlineProductRepository.findByProductId(productId)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("OfflineProduct not found: " + productId));
    }

    private OfflineProductDto toDto(OfflineProduct offlineProduct) {
        int receiving = offlineProduct.getReceivingQuantity() != null
                ? offlineProduct.getReceivingQuantity() : 0;
        int shipping = offlineProduct.getShippingQuantity() != null
                ? offlineProduct.getShippingQuantity() : 0;
        return OfflineProductDto.builder()
            .id(offlineProduct.getId())
            .productId(offlineProduct.getProductId())
            .categoryId(offlineProduct.getCategoryId())
            .categoryTitle(offlineProduct.getCategoryTitle())
            .title(offlineProduct.getTitle())
            .priceUnit(offlineProduct.getPriceUnit())
            .priceValue(offlineProduct.getPriceValue())
            .barcode(offlineProduct.getBarcode())
            .createdAt(offlineProduct.getCreatedAt())
            .updatedAt(offlineProduct.getUpdatedAt())
            .linkTableName(offlineProduct.getLinkTableName())
            .linkId(offlineProduct.getLinkId())
            .receivingQuantity(receiving)
            .shippingQuantity(shipping)
            .stockQuantity(receiving - shipping)
            .build();
    }

    @Override
    @Transactional
    public void simpleRegisterOfflineProduct(SimpleRegisterOfflineProductDto dto) {
        OfflineProduct offlineProduct = offlineProductRepository.findById(dto.getOfflineProductId())
                .orElseThrow(() -> new RuntimeException("OfflineProduct not found: " + dto.getOfflineProductId()));
        if (offlineProduct.getLinkTableName() != null) {
            throw new RuntimeException("이미 연결된 오프라인 상품입니다.");
        }

        switch (dto.getLinkTableName()) {
            case "Sealed" -> sealedProductService.simpleRegisterFromOffline(offlineProduct);
            case "Manual" -> manualProductService.simpleRegisterFromOffline(offlineProduct);
            case "Supply" -> adminSupplyService.simpleRegisterFromOffline(offlineProduct);
            default -> throw new RuntimeException("Invalid link table name: " + dto.getLinkTableName());
        }
    }

    @Override
    @Transactional
    public OfflineProductReceivingHistoryDto registerReceiving(OfflineProductReceivingRequest request) {
        String receivingManager = authenticatedUserProvider.resolveCurrentUserName()
                .orElseThrow(() -> new RuntimeException("로그인 정보가 없습니다."));
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("입고할 품목이 없습니다.");
        }

        Set<Long> productIds = new HashSet<>();
        for (OfflineProductReceivingRequest.OfflineProductReceivingItemRequest item : request.getItems()) {
            if (item.getProductId() == null) {
                throw new RuntimeException("상품 ID가 필요합니다.");
            }
            if (item.getReceivingQuantity() == null || item.getReceivingQuantity() == 0) {
                throw new RuntimeException("입고 수량은 0이 될 수 없습니다. (음수는 차감 가능)");
            }
            if (!productIds.add(item.getProductId())) {
                throw new RuntimeException("동일한 상품이 중복되었습니다: " + item.getProductId());
            }
        }

        Map<Long, OfflineProduct> productMap = offlineProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(OfflineProduct::getId, Function.identity()));
        if (productMap.size() != productIds.size()) {
            Set<Long> missing = new HashSet<>(productIds);
            missing.removeAll(productMap.keySet());
            throw new RuntimeException("OfflineProduct not found: " + missing);
        }

        OfflineProductReceivingHistory history = OfflineProductReceivingHistory.builder()
                .receivingManager(receivingManager)
                .build();

        for (OfflineProductReceivingRequest.OfflineProductReceivingItemRequest itemReq : request.getItems()) {
            OfflineProduct product = productMap.get(itemReq.getProductId());
            int current = product.getReceivingQuantity() != null ? product.getReceivingQuantity() : 0;
            product.setReceivingQuantity(current + itemReq.getReceivingQuantity());
            receivingOnlineStockAdjuster.applyReceivingDelta(product, itemReq.getReceivingQuantity());

            OfflineProductReceivingItem item = OfflineProductReceivingItem.builder()
                    .productId(itemReq.getProductId())
                    .receivingQuantity(itemReq.getReceivingQuantity())
                    .build();
            history.addItem(item);
        }

        OfflineProductReceivingHistory saved = receivingHistoryRepository.save(history);
        return toReceivingHistoryDto(saved, productMap);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfflineProductReceivingHistoryDto> getReceivingHistories(Pageable pageable) {
        Page<OfflineProductReceivingHistory> page =
                receivingHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, OfflineProduct> productMap = loadProductsForHistories(page.getContent());
        return page.map(history -> toReceivingHistoryDto(history, productMap));
    }

    @Override
    @Transactional(readOnly = true)
    public OfflineProductReceivingHistoryDto getReceivingHistory(Long id) {
        OfflineProductReceivingHistory history = receivingHistoryRepository.findWithItemsById(id)
                .orElseThrow(() -> new RuntimeException("입고 내역을 찾을 수 없습니다: " + id));
        Map<Long, OfflineProduct> productMap = loadProductsForHistories(List.of(history));
        return toReceivingHistoryDto(history, productMap);
    }

    private Map<Long, OfflineProduct> loadProductsForHistories(
            List<OfflineProductReceivingHistory> histories) {
        Set<Long> productIds = new HashSet<>();
        for (OfflineProductReceivingHistory history : histories) {
            for (OfflineProductReceivingItem item : history.getItems()) {
                productIds.add(item.getProductId());
            }
        }
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return offlineProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(OfflineProduct::getId, Function.identity()));
    }

    private OfflineProductReceivingHistoryDto toReceivingHistoryDto(
            OfflineProductReceivingHistory history,
            Map<Long, OfflineProduct> productMap) {
        List<OfflineProductReceivingHistoryDto.OfflineProductReceivingItemDto> itemDtos = new ArrayList<>();
        for (OfflineProductReceivingItem item : history.getItems()) {
            OfflineProduct product = productMap.get(item.getProductId());
            itemDtos.add(OfflineProductReceivingHistoryDto.OfflineProductReceivingItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .tossProductId(product != null ? product.getProductId() : null)
                    .title(product != null ? product.getTitle() : null)
                    .receivingQuantity(item.getReceivingQuantity())
                    .build());
        }
        return OfflineProductReceivingHistoryDto.builder()
                .id(history.getId())
                .createdAt(history.getCreatedAt())
                .receivingManager(history.getReceivingManager())
                .itemCount(itemDtos.size())
                .items(itemDtos)
                .build();
    }

    @Override
    @Transactional
    public OfflineShippingReconcileResultDto reconcileShippingQuantities() {
        OfflineShippingReconcileResultDto result =
                shippingQuantityAdjuster.reconcileAbsoluteShippingQuantities();
        log.info(
                "오프라인 출고 전량 재집계 완료: updated={}, salesTitles={}, unmatched={}, duplicateGroups={}",
                result.getUpdatedProductCount(),
                result.getSalesTitleCount(),
                result.getUnmatchedSalesTitleCount(),
                result.getDuplicateTitleGroupCount());
        return result;
    }

    @Override
    @Transactional
    public OfflineStockSyncResultDto syncStockFromCurrentQuantities() {
        OfflineStockSyncResultDto result = shippingQuantityAdjuster.syncStockFromCurrentQuantities();
        log.info("오프라인 재고 동기화 완료(현재 입·출고 기준): processed={}", result.getProcessedProductCount());
        return result;
    }
}
