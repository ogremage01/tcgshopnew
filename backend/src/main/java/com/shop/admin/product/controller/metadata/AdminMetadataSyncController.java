package com.shop.admin.product.controller.metadata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.log.sync.dto.LastSyncTimesDto;
import com.shop.log.sync.dto.SyncLogDto;
import com.shop.log.sync.service.SyncLogService;
import com.shop.scheduler.metadata.application.MetadataService;
import com.shop.scheduler.metadata.dto.MetadataSyncResponse;
import com.shop.scheduler.price.application.PriceService;
import com.shop.scheduler.price.service.link.PriceLinkService;
import com.shop.scheduler.price.service.union.UnionPriceIngestionService;
import com.shop.scheduler.stock.application.StockService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetadataSyncController {

    // 동기화 관련 기능입니다. (가격 동기화, 메타데이터 동기화, 가격 링크 매칭)

    private final PriceService priceService;
    private final SyncLogService syncLogService;
    private final PriceLinkService priceLinkService;
    private final MetadataService metadataService;
    private final UnionPriceIngestionService unionPriceIngestionService;
    private final StockService stockService;

    /**
     * 전체 메타데이터 동기화를 수행합니다.
     *
     * @return 동기화 실행 결과
     */

    @PostMapping("/sync")
    public ResponseEntity<MetadataSyncResponse> syncMetadata() {
        boolean started = metadataService.syncAll();
        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(MetadataSyncResponse.alreadyRunning());
        }
        return ResponseEntity.ok(MetadataSyncResponse.completed());
    }

    /**
     * TcgPPrice 동기화를 수행합니다.
     *
     * @return 동기화 실행 결과
     */
    @PostMapping("/tcg-p-prices/sync")
    public ResponseEntity<String> syncTcgPPrices() {
        if (!priceService.startSyncPrices()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("TcgPPrice sync is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("TcgPPrice sync started.");
    }

    /**
     * 동기화 로그를 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 동기화 로그 목록
     */
    @GetMapping("/sync/log")
    public ResponseEntity<Page<SyncLogDto>> getSyncLog(Pageable pageable) {
        Page<SyncLogDto> result = syncLogService.getSyncLogs(pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 마지막 동기화 시간을 조회합니다.
     *
     * @return 마지막 동기화 시간 목록
     */
    @GetMapping("/sync/last-time")
    public ResponseEntity<LastSyncTimesDto> getLastSyncTime() {
        return ResponseEntity.ok(syncLogService.getLastSyncTimes());
    }

    /**
     * Open Binder 가격 동기화를 수행합니다.
     *
     * @return 동기화 실행 결과
     */
    @PostMapping("/sync/openbinder-prices")
    public ResponseEntity<String> syncOpenBinderPrices() {
        if (!priceService.startSyncOpenBinderPrices()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Open Binder price sync is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Open Binder price sync started. union_prices will be updated automatically after completion.");
    }

    /**
     * check_code/check_code_refined만 초기화 후 재계산합니다. ID는 건드리지 않습니다.
     */
    @PostMapping("/sync/price-check-code-rebuild")
    public ResponseEntity<String> rebuildCheckCodes() {
        if (!priceLinkService.startRebuildCheckCodes()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Another price link job is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("check_code rebuild started in background. Check sync logs.");
    }

    /**
     * NULL 초기화 없이 check_code를 재계산합니다. 행마다 check_code와 check_code_refined가 같으면 둘 다
     * 갱신하고,
     * 다르면 check_code_refined는 유지한 채 check_code만 갱신합니다.
     */
    @PostMapping("/sync/price-check-code-rebuild-selective")
    public ResponseEntity<String> rebuildCheckCodesSelective() {
        if (!priceLinkService.startRebuildCheckCodesSelective()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Another price link job is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Selective check_code rebuild started in background. Check sync logs.");
    }

    /**
     * tcgPPriceId/obPriceId만 초기화 후 재연결합니다. check_code는 건드리지 않습니다.
     */
    @PostMapping("/sync/price-link-rebuild")
    public ResponseEntity<String> rebuildLinks() {
        if (!priceLinkService.startRebuildLinks()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Another price link job is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Link rebuild started in background. Check sync logs.");
    }

    /**
     * check_code 재빌드 후 링크 재연결을 순차 실행합니다 (전체 재빌드).
     */
    @PostMapping("/sync/price-full-rebuild")
    public ResponseEntity<String> fullRebuild() {
        if (!priceLinkService.startFullRebuild()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Another price link job is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Full rebuild started in background. Check sync logs.");
    }

    /**
     * NULL 미연결 행을 커서 없이 재스캔해 링크한다(갭 정비).
     */
    @PostMapping("/sync/price-link-rescan-nulls")
    public ResponseEntity<String> syncPriceLinkRescanNulls() {
        if (!priceLinkService.startSyncPriceLinkRescanNulls()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Another price link job is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Price link rescan-nulls started in background.");
    }

    /**
     * 모든 가격을 union_prices에 저장합니다.
     */
    @PostMapping("/sync/price-union-save")
    public ResponseEntity<String> saveAllPricesToUnion() {
        if (!unionPriceIngestionService.startSaveAllPricesToUnion()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Union price save is already running.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Union price save started in background.");
    }

    /**
     * union_prices.public_id 누락 데이터를 즉시 백필합니다.
     */
    @PostMapping("/sync/price-union-public-id-backfill")
    public ResponseEntity<String> backfillUnionPublicIds() {
        int updated = unionPriceIngestionService.backfillMissingPublicIds();
        return ResponseEntity.ok("Union public_id backfill completed. updated=" + updated);
    }

    /**
     * 자동 재고 충전(current_visible_stock = LEAST(max_visible_stock, total_stock))을 백그라운드에서 실행합니다.
     */
    @PostMapping("/sync/stock-charge")
    public ResponseEntity<String> syncStockCharge() {
        if (!stockService.startSyncStock("admin.stock")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("재고 충전이 이미 진행중입니다.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("재고 충전을 백그라운드에서 시작했습니다.");
    }
}
