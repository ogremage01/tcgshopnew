package com.shop.scheduler.image.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.shop.card.entity.MtgPrice;
import com.shop.card.metadata.support.MtgImageKey;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.log.sync.event.SyncLogEvent;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient.ScryfallCardImages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScryfallImageDownloadServiceImpl implements ScryfallImageDownloadService {

    private static final String IMAGE_DOWNLOAD_LOG_SOURCE = "api.scryfall.com";
    private static final String IMAGE_SOURCE_SCRYFALL = "SCRYFALL";
    private static final String GAME_SLUG = "mtg";
    private static final int READ_LIMIT = 5000;

    private final ScryfallCardClient scryfallCardClient;
    private final MtgPriceRepository mtgPriceRepository;
    private final UnionPriceRepository unionPriceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean downloadRunning = new AtomicBoolean(false);

    @Value("${file.path.card-images}")
    private String imageDownloadDirectory;

    @Override
    public boolean isDownloadRunning() {
        return downloadRunning.get();
    }

    @Async
    @Override
    public void downloadScryfallImagesAsync() {
        log.info("Scryfall image download async request accepted.");
        downloadScryfallImages();
    }

    @Override
    public boolean downloadScryfallImages() {
        if (!downloadRunning.compareAndSet(false, true)) {
            log.warn("Scryfall image download already running. Skip.");
            return false;
        }

        LocalDateTime startTime = LocalDateTime.now();
        String result = "failure";
        DownloadStats stats = new DownloadStats();
        String message = "";

        try {
            log.info("Scryfall image download started. directory={}", imageDownloadDirectory);
            downloadMtgImages(stats);
            result = stats.failed == 0 ? "success" : "partial_success";
            message = stats.message();
            log.info("Scryfall image download finished. result={}, {}", result, message);
        } catch (Exception e) {
            message = stats.message() + "; "
                    + (e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                            : e.getClass().getSimpleName());
            log.error("Scryfall image download failed. {}", message, e);
        } finally {
            downloadRunning.set(false);
            eventPublisher.publishEvent(new SyncLogEvent("scryfall-image-download", IMAGE_DOWNLOAD_LOG_SOURCE,
                    startTime, LocalDateTime.now(), result, message));
            log.info("Scryfall image download sync log published. result={}, {}", result, message);
        }
        return true;
    }

    private void downloadMtgImages(DownloadStats stats) {
        long lastId = 0L;
        while (true) {
            List<MtgPrice> targets = mtgPriceRepository.findOpenBinderImageDownloadTargets(lastId,
                    PageRequest.of(0, READ_LIMIT));
            if (targets.isEmpty()) {
                log.info("Scryfall MTG image download: no more targets. lastId={}", lastId);
                return;
            }
            log.info("Scryfall MTG batch fetched. lastId={}, batchSize={}", lastId, targets.size());
            stats.targets += targets.size();
            for (MtgPrice target : targets) {
                DownloadOutcome outcome = downloadMtgImage(target);
                if (outcome == DownloadOutcome.SKIPPED) {
                    stats.skipped++;
                    continue;
                }
                if (outcome == DownloadOutcome.SUCCESS) {
                    stats.downloaded++;
                } else {
                    stats.failed++;
                }
            }
            lastId = targets.get(targets.size() - 1).getId();
            log.info("Scryfall MTG batch processed. lastId={}, {}", lastId, stats.message());
            if (targets.size() < READ_LIMIT) {
                return;
            }
        }
    }

    private DownloadOutcome downloadMtgImage(MtgPrice target) {
        String imageKey = MtgImageKey.from(target);
        if (imageKey.isBlank()) {
            log.warn("Scryfall MTG image key empty. id={}, set={}, code={}",
                    target.getId(), target.getSet(), target.getCode());
            return DownloadOutcome.FAILED;
        }

        Path pngPath = imagePath(imageKey, "en", "png");
        if (Files.exists(pngPath)) {
            log.debug("Scryfall skip: OpenBinder png already exists. path={}", pngPath);
            return DownloadOutcome.SKIPPED;
        }

        Path jpgPath = imagePath(imageKey, "en", "jpg");
        if (Files.exists(jpgPath)) {
            boolean doubleSided = Files.exists(imagePath(imageKey, "enback", "jpg"))
                    || Boolean.TRUE.equals(target.getIsDoubleSided());
            markSuccess(target, imageKey, doubleSided);
            return DownloadOutcome.SUCCESS;
        }

        if (tryDownloadFromSet(target, imageKey, jpgPath, target.getSet())) {
            return DownloadOutcome.SUCCESS;
        }
        String strippedSet = stripLeadingT(target.getSet());
        if (strippedSet != null) {
            log.info("Scryfall retry without leading t. id={}, originalSet={}, retrySet={}, code={}",
                    target.getId(), target.getSet(), strippedSet, target.getCode());
            if (tryDownloadFromSet(target, imageKey, jpgPath, strippedSet)) {
                return DownloadOutcome.SUCCESS;
            }
        }
        return DownloadOutcome.FAILED;
    }

    private boolean tryDownloadFromSet(MtgPrice target, String imageKey, Path jpgPath, String set) {
        ScryfallCardImages images = scryfallCardClient.fetchCardImages(set, target.getCode())
                .orElse(null);
        if (images == null || !images.hasFront()) {
            log.warn("Scryfall card image uri missing. id={}, set={}, code={}",
                    target.getId(), set, target.getCode());
            return false;
        }
        if (!saveImage(jpgPath, images.frontNormalUrl())) {
            return false;
        }
        boolean downloadedBack = false;
        if (images.hasBack()) {
            downloadedBack = saveImage(imagePath(imageKey, "enback", "jpg"), images.backNormalUrl());
        }
        boolean doubleSided = downloadedBack || Boolean.TRUE.equals(target.getIsDoubleSided());
        markSuccess(target, imageKey, doubleSided);
        return true;
    }

    /**
     * Scryfall 토큰 세트 코드(tneo 등)가 원 세트(neo)로 조회돼야 하는 경우.
     * 선행 t/T가 있고 제거 후에도 코드가 남으면 그 값을 반환한다.
     */
    static String stripLeadingT(String set) {
        if (set == null) {
            return null;
        }
        String trimmed = set.trim();
        if (trimmed.length() < 2) {
            return null;
        }
        char first = trimmed.charAt(0);
        if (first != 't' && first != 'T') {
            return null;
        }
        String stripped = trimmed.substring(1).trim();
        return stripped.isEmpty() ? null : stripped;
    }

    private boolean saveImage(Path savePath, String sourceUrl) {
        if (Files.exists(savePath)) {
            return true;
        }
        try {
            byte[] imageBytes = scryfallCardClient.downloadImage(sourceUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("Scryfall image download returned no content. path={}, url={}", savePath, sourceUrl);
                return false;
            }
            Files.createDirectories(savePath.getParent());
            Files.write(savePath, imageBytes);
            log.debug("Scryfall image saved. path={}, bytes={}", savePath, imageBytes.length);
            return true;
        } catch (Exception e) {
            log.warn("Scryfall image save failed. path={}, url={}, msg={}", savePath, sourceUrl, e.getMessage());
            return false;
        }
    }

    private void markSuccess(MtgPrice target, String imageKey, boolean doubleSided) {
        target.setDownloaded(true);
        mtgPriceRepository.save(target);
        String checkCodeRefined = target.getCheckCodeRefined();
        if (checkCodeRefined == null || checkCodeRefined.isBlank()) {
            return;
        }
        unionPriceRepository.updateImageMetaByCheckCodeRefined(
                checkCodeRefined, IMAGE_SOURCE_SCRYFALL, imageKey, doubleSided);
    }

    private Path imagePath(String imageKey, String suffix, String extension) {
        return Path.of(imageDownloadDirectory, "mtg-kr", GAME_SLUG, imageKey + "-" + suffix + "." + extension);
    }

    private enum DownloadOutcome {
        SUCCESS, FAILED, SKIPPED
    }

    private static class DownloadStats {
        int targets;
        int downloaded;
        int skipped;
        int failed;

        String message() {
            return "targets=" + targets + ",downloaded=" + downloaded + ",skipped=" + skipped + ",failed=" + failed;
        }
    }
}
