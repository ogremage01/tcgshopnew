package com.shop.scheduler.price.application.selling;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntSupplier;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.shop.log.sync.event.SyncLogEvent;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.search.service.ProductSearchMapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class CardSellingPriceUpdateServiceImpl implements CardSellingPriceUpdateService {

    private static final int SEARCH_SYNC_PAGE_SIZE = 500;

    private static final String SYNC_TARGET = "card:selling-price";
    private static final String SYNC_SOURCE = "internal";

    private final CardProductRepository cardProductRepository;
    private final ProductSearchMapService productSearchMapService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean cardSellingPriceUpdateRunning = new AtomicBoolean(false);

    @Override
    public boolean startUpdateCardCalculatedLinkedPrice() {
        return startUpdateAsync(() -> updateCardCalculatedLinkedPrice());
    }

    @Override
    public boolean startUpdateCardCalculatedLinkedPriceForGrade(String grade) {
        return startUpdateAsync(() -> updateCardCalculatedLinkedPriceForGrade(grade));
    }

    @Override
    public void updateCardCalculatedLinkedPrice() {
        bulkUpdateAndSync(
                cardProductRepository::bulkUpdateCalculatedLinkedPrice,
                null,
                "all grades");
    }

    @Override
    public void updateCardCalculatedLinkedPriceForGrade(String grade) {
        bulkUpdateAndSync(
                () -> cardProductRepository.bulkUpdateCalculatedLinkedPriceByGrade(grade),
                grade,
                "grade=" + grade);
    }

    private boolean startUpdateAsync(Runnable updateTask) {
        if (!cardSellingPriceUpdateRunning.compareAndSet(false, true)) {
            log.warn("Card selling price update is already running. Skip this request.");
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                updateTask.run();
            } finally {
                cardSellingPriceUpdateRunning.set(false);
            }
        });
        return true;
    }

    private void bulkUpdateAndSync(IntSupplier bulkUpdate, String grade, String scopeLabel) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        Integer updatedCount = transactionTemplate.execute(status -> bulkUpdate.getAsInt());
        long bulkMs = System.currentTimeMillis() - startMs;

        if (updatedCount == null || updatedCount == 0) {
            log.info("No card products updated for calculated linked price ({})", scopeLabel);
            return;
        }

        if (grade == null) {
            syncPriceLinkedSearchMapsInPages();
        } else {
            syncPriceLinkedSearchMapsInPagesByGrade(grade);
        }

        long totalMs = System.currentTimeMillis() - startMs;
        String msg = String.format("updated=%d, %s, bulkSql=%dms, total=%dms", updatedCount, scopeLabel, bulkMs, totalMs);
        eventPublisher.publishEvent(new SyncLogEvent(SYNC_TARGET, SYNC_SOURCE, startTime, LocalDateTime.now(), "success", msg));
    }

    private void syncPriceLinkedSearchMapsInPages() {
        syncPriceLinkedSearchMapsInPages(
                pageable -> cardProductRepository.findPageByIsPriceLinkedTrue(pageable));
    }

    private void syncPriceLinkedSearchMapsInPagesByGrade(String grade) {
        syncPriceLinkedSearchMapsInPages(
                pageable -> cardProductRepository.findPageByIsPriceLinkedTrueAndCondition(grade, pageable));
    }

    private void syncPriceLinkedSearchMapsInPages(Function<PageRequest, Page<CardProduct>> pageFetcher) {
        int pageNumber = 0;
        Page<CardProduct> page;
        do {
            page = pageFetcher.apply(
                    PageRequest.of(pageNumber, SEARCH_SYNC_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            productSearchMapService.syncProductSearchMapBulk(page.getContent());
            pageNumber++;
        } while (page.hasNext());
    }

    @Override
    @Transactional
    public void raiseMinimumPriceForCardProducts(Long minimumPrice, String game) {
        cardProductRepository.raiseMinimumPriceForCardProducts(minimumPrice, game);
    }

    @Override
    @Transactional
    public void dropMinimumPriceForCardProducts(Long newMinimumPrice, Long lastMinimumPrice, String game) {
        cardProductRepository.dropMinimumPriceForCardProducts(newMinimumPrice, lastMinimumPrice, game);
    }
}
