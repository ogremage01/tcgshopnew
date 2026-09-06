package com.shop.scheduler.image.application;

import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

import com.shop.card.entity.TcgPPrice;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.log.sync.event.SyncLogEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;
import com.shop.scheduler.price.service.union.UnionPriceIngestionService;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class TcgPImageDownloadServiceImpl implements TcgPImageDownloadService {

    private static final String IMAGE_DOWNLOAD_LOG_SOURCE = "external-card-image";
    private static final int WEBCLIENT_MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;
    private final WebClient webClient = WebClient.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs()
                    .maxInMemorySize(WEBCLIENT_MAX_IN_MEMORY_BYTES))
            .build();
    String tcgPImagesUrl = "";
    private final TcgPPriceRepository tcgPPriceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UnionPriceIngestionService unionPriceIngestionService;
    private final AtomicBoolean downloadRunning = new AtomicBoolean(false);

    @Value("${file.path.card-images}")
    private String imageDownloadDirectory;

    @Override
    public boolean isDownloadRunning() { return downloadRunning.get(); }

    @Async
    @Override
    public void downloadTcgPImagesAsync() { downloadTcgPImages(); }

    @Override
    public boolean downloadTcgPImages() {
        if (!downloadRunning.compareAndSet(false, true)) {
            log.warn("TCG-P image download already running. Skip.");
            return false;
        }
        LocalDateTime startTime = LocalDateTime.now();
        String result = "failure";
        String message = "";
        int targetCount = 0;
        try {
            if (tcgPImagesUrl == null || tcgPImagesUrl.isBlank()) {
                log.warn("Portfolio snapshot: external TCG image source is omitted.");
                result = "failure";
                message = "external image source omitted in public snapshot";
                return true;
            }
            List<TcgPPrice> targets = tcgPPriceRepository.findAllCardImageDownloadTargets();
            targetCount = targets.size();
            log.info("TCG-P image download started. targets={}", targetCount);
            for (TcgPPrice target : targets) {
                boolean primaryDownloaded = false;
                String productId = target.getProductId().toString();
                String game = target.getGame();
                String savePath = imageDownloadDirectory + game + "/" + productId + ".jpg";
                String savePath2 = imageDownloadDirectory + game + "/" + productId + "_1.jpg";
                String imageUrl = tcgPImagesUrl.replace("{productId}", productId);
                String imageUrl2 = tcgPImagesUrl.replace("{productId}", productId + "_1");
                Path path = Path.of(savePath);
                Path path2 = Path.of(savePath2);
                if (Files.exists(path)) continue;
                try {
                    byte[] imageBytes = webClient.get().uri(imageUrl).exchangeToMono(res -> {
                        if (!res.statusCode().is2xxSuccessful()) { markPrimaryImageDownloadFailed(target.getProductId()); return Mono.empty(); }
                        String contentType = res.headers().contentType().map(Object::toString).orElse("");
                        if (!contentType.startsWith("image")) { markPrimaryImageDownloadFailed(target.getProductId()); return Mono.empty(); }
                        return res.bodyToMono(byte[].class);
                    }).block();
                    if (imageBytes == null || imageBytes.length == 0) continue;
                    Files.createDirectories(path.getParent());
                    Files.write(path, imageBytes);
                    markPrimaryImageDownloadSuccess(target.getProductId());
                    primaryDownloaded = true;
                    target.setIsDoubleSided(false);
                } catch (Exception e) {
                    log.error("다운로드 실패 productId={}", productId, e);
                }
                try {
                    if (Files.exists(path2)) continue;
                    byte[] imageBytes = webClient.get().uri(imageUrl2).exchangeToMono(res -> {
                        if (!res.statusCode().is2xxSuccessful()) { markNotDoubleSided(target.getProductId()); target.setIsDoubleSided(false); return Mono.empty(); }
                        String contentType = res.headers().contentType().map(Object::toString).orElse("");
                        if (!contentType.startsWith("image")) return Mono.empty();
                        return res.bodyToMono(byte[].class);
                    }).block();
                    if (imageBytes == null || imageBytes.length == 0) continue;
                    Files.createDirectories(path2.getParent());
                    Files.write(path2, imageBytes);
                    markDoubleSided(target.getProductId());
                    markPrimaryImageDownloadSuccess(target.getProductId());
                    target.setIsDoubleSided(true);
                } catch (Exception e) {
                    log.error("다운로드 실패 productId={}", productId, e);
                }
                if (primaryDownloaded && target.getIsDoubleSided() != null) saveUnionImageMeta(target);
            }
            result = "success";
            message = "targets=" + targetCount;
        } catch (Exception e) {
            message = "targets=" + targetCount + "; "
                    + (e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            downloadRunning.set(false);
            LocalDateTime endTime = LocalDateTime.now();
            eventPublisher.publishEvent(new SyncLogEvent("tcg-p-image-download", IMAGE_DOWNLOAD_LOG_SOURCE, startTime, endTime, result, message != null ? message : ""));
        }
        return true;
    }

    private void markPrimaryImageDownloadSuccess(Long productId) {
        List<TcgPPrice> rows = tcgPPriceRepository.findAllByProductId(productId);
        if (rows.isEmpty()) return;
        rows.forEach(row -> row.setDownloaded(true));
        tcgPPriceRepository.saveAll(rows);
    }

    private void markPrimaryImageDownloadFailed(Long productId) {
        List<TcgPPrice> rows = tcgPPriceRepository.findAllByProductId(productId);
        if (rows.isEmpty()) return;
        rows.forEach(row -> row.setDownloaded(false));
        tcgPPriceRepository.saveAll(rows);
    }

    private void markNotDoubleSided(Long productId) {
        List<TcgPPrice> rows = tcgPPriceRepository.findAllByProductId(productId);
        if (rows.isEmpty()) return;
        rows.forEach(row -> row.setIsDoubleSided(false));
        tcgPPriceRepository.saveAll(rows);
    }

    private void markDoubleSided(Long productId) {
        List<TcgPPrice> rows = tcgPPriceRepository.findAllByProductId(productId);
        if (rows.isEmpty()) return;
        rows.forEach(row -> row.setIsDoubleSided(true));
        tcgPPriceRepository.saveAll(rows);
    }

    private void saveUnionImageMeta(TcgPPrice target) {
        if (target == null || target.getProductId() == null) return;
        unionPriceIngestionService.saveTcgPriceImageUrlToUnion(target);
    }
}
