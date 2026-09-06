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
import org.springframework.web.reactive.function.client.WebClient;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.metadata.support.FabSetCode;
import com.shop.card.metadata.support.MtgImageKey;
import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.log.sync.event.SyncLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenBinderImageDownloadServiceImpl implements OpenBinderImageDownloadService {

    private static final String IMAGE_DOWNLOAD_LOG_SOURCE = "external-card-image";
    private static final String SOURCE_URL_TEMPLATE = "";
    private static final int READ_LIMIT = 5000;
    private static final int WEBCLIENT_MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;
    private static final List<String> MTG_SUFFIXES = List.of("en", "ko", "enback", "koback");

    private final WebClient webClient = WebClient.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs()
                    .maxInMemorySize(WEBCLIENT_MAX_IN_MEMORY_BYTES))
            .build();
    private final FabPriceRepository fabPriceRepository;
    private final MtgPriceRepository mtgPriceRepository;
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
    public void downloadOpenBinderImagesAsync() {
        log.info("OpenBinder image download async request accepted.");
        downloadOpenBinderImages();
    }

    @Override
    public boolean downloadOpenBinderImages() {
        if (!downloadRunning.compareAndSet(false, true)) {
            log.warn("OpenBinder image download already running. Skip.");
            return false;
        }

        LocalDateTime startTime = LocalDateTime.now();
        String result = "failure";
        DownloadStats stats = new DownloadStats();
        String message = "";

        try {
            if (SOURCE_URL_TEMPLATE.isBlank()) {
                result = "failure";
                message = "external image source omitted in public snapshot";
                log.warn("Portfolio snapshot: external card image source is omitted.");
                return true;
            }
            log.info("OpenBinder image download started. directory={}", imageDownloadDirectory);
            downloadFabImages(stats);
            log.info("OpenBinder FAB image download phase finished. {}", stats.message());
            downloadMtgImages(stats);
            result = stats.failed == 0 ? "success" : "partial_success";
            message = stats.message();
            log.info("OpenBinder image download finished. result={}, {}", result, message);
        } catch (Exception e) {
            message = stats.message() + "; "
                    + (e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                            : e.getClass().getSimpleName());
            log.error("OpenBinder image download failed. {}", message, e);
        } finally {
            downloadRunning.set(false);
            eventPublisher.publishEvent(new SyncLogEvent("openbinder-image-download", IMAGE_DOWNLOAD_LOG_SOURCE,
                    startTime, LocalDateTime.now(), result, message));
            log.info("OpenBinder image download sync log published. result={}, {}", result, message);
        }
        return true;
    }

    private void downloadFabImages(DownloadStats stats) {
        log.info("OpenBinder FAB image download phase started.");
        long lastId = 0L;
        while (true) {
            List<FabPrice> targets = fabPriceRepository.findOpenBinderImageDownloadTargets(lastId,
                    PageRequest.of(0, READ_LIMIT));
            if (targets.isEmpty()) {
                log.info("OpenBinder FAB image download: no more targets. lastId={}", lastId);
                return;
            }
            log.info("OpenBinder FAB batch fetched. lastId={}, batchSize={}", lastId, targets.size());
            stats.targets += targets.size();
            for (FabPrice target : targets) {
                if (downloadFabImage(target)) {
                    target.setDownloaded(true);
                    fabPriceRepository.save(target);
                    stats.downloaded++;
                } else {
                    target.setDownloaded(false);
                    fabPriceRepository.save(target);
                    stats.failed++;
                }
            }
            lastId = targets.get(targets.size() - 1).getId();
            log.info("OpenBinder FAB batch processed. lastId={}, {}", lastId, stats.message());
            if (targets.size() < READ_LIMIT) {
                return;
            }
        }
    }

    private boolean downloadFabImage(FabPrice target) {
        String imageKey = buildFabImageKey(target);
        if (imageKey.isBlank()) {
            log.warn("OpenBinder FAB image key empty. id={}, set={}, code={}",
                    target.getId(), target.getSet(), target.getCode());
            return false;
        }
        return downloadRequiredImage("fab", imageKey, "en");
    }

    private void downloadMtgImages(DownloadStats stats) {
        log.info("OpenBinder MTG image download phase started.");
        long lastId = 0L;
        while (true) {
            List<MtgPrice> targets = mtgPriceRepository.findOpenBinderImageDownloadTargets(lastId,
                    PageRequest.of(0, READ_LIMIT));
            if (targets.isEmpty()) {
                log.info("OpenBinder MTG image download: no more targets. lastId={}", lastId);
                return;
            }
            log.info("OpenBinder MTG batch fetched. lastId={}, batchSize={}", lastId, targets.size());
            stats.targets += targets.size();
            for (MtgPrice target : targets) {
                if (downloadMtgImageSet(target)) {
                    target.setDownloaded(true);
                    mtgPriceRepository.save(target);
                    stats.downloaded++;
                } else {
                    target.setDownloaded(false);
                    mtgPriceRepository.save(target);
                    stats.failed++;
                }
            }
            lastId = targets.get(targets.size() - 1).getId();
            log.info("OpenBinder MTG batch processed. lastId={}, {}", lastId, stats.message());
            if (targets.size() < READ_LIMIT) {
                return;
            }
        }
    }

    private boolean downloadMtgImageSet(MtgPrice target) {
        String imageKey = MtgImageKey.from(target);
        if (imageKey.isBlank()) {
            log.warn("OpenBinder MTG image key empty. id={}, set={}, code={}",
                    target.getId(), target.getSet(), target.getCode());
            return false;
        }

        boolean primaryDownloaded = false;
        for (String suffix : MTG_SUFFIXES) {
            boolean downloaded = downloadImage("mtg", imageKey, suffix);
            if ("en".equals(suffix)) {
                primaryDownloaded = downloaded;
            }
        }
        return primaryDownloaded;
    }

    private boolean downloadRequiredImage(String gameSlug, String imageKey, String suffix) {
        return downloadImage(gameSlug, imageKey, suffix);
    }

    private boolean downloadImage(String gameSlug, String imageKey, String suffix) {
        Path savePath = Path.of(imageDownloadDirectory, "mtg-kr", gameSlug, imageKey + "-" + suffix + ".png");
        if (Files.exists(savePath)) {
            log.debug("OpenBinder image already exists. path={}", savePath);
            return true;
        }

        if (SOURCE_URL_TEMPLATE.isBlank()) {
            log.warn("Portfolio snapshot: external card image source is omitted. game={}, imageKey={}, suffix={}",
                    gameSlug, imageKey, suffix);
            return false;
        }
        String sourceUrl = SOURCE_URL_TEMPLATE.formatted(imageKey, suffix);
        try {
            byte[] imageBytes = webClient.get().uri(sourceUrl).exchangeToMono(res -> {
                if (!res.statusCode().is2xxSuccessful()) {
                    log.warn("OpenBinder image HTTP non-success. status={}, url={}",
                            res.statusCode().value(), sourceUrl);
                    return Mono.empty();
                }
                String contentType = res.headers().contentType().map(Object::toString).orElse("");
                if (!contentType.startsWith("image")) {
                    log.warn("OpenBinder image invalid content-type. contentType={}, url={}",
                            contentType, sourceUrl);
                    return Mono.empty();
                }
                return res.bodyToMono(byte[].class);
            }).block();

            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("OpenBinder image download returned no content. game={}, imageKey={}, suffix={}, url={}",
                        gameSlug, imageKey, suffix, sourceUrl);
                return false;
            }

            Files.createDirectories(savePath.getParent());
            Files.write(savePath, imageBytes);
            log.debug("OpenBinder image saved. path={}, bytes={}", savePath, imageBytes.length);
            return true;
        } catch (Exception e) {
            log.warn("OpenBinder image download failed. game={}, imageKey={}, suffix={}, url={}, msg={}",
                    gameSlug, imageKey, suffix, sourceUrl, e.getMessage());
            return false;
        }
    }

    private static String buildFabImageKey(FabPrice fabPrice) {
        if (fabPrice == null) {
            return "";
        }
        String rawSet = fabPrice.getSet();
        if (rawSet == null || rawSet.isBlank()) {
            return "";
        }
        String set = FabSetCode.toSourceCode(rawSet.trim());
        String code = fabPrice.getCode() == null ? "" : fabPrice.getCode().trim();
        if (set.isBlank() || code.isBlank()) {
            return "";
        }
        return set + "-" + code;
    }

    private static class DownloadStats {
        int targets;
        int downloaded;
        int failed;

        String message() {
            return "targets=" + targets + ",downloaded=" + downloaded + ",failed=" + failed;
        }
    }
}
