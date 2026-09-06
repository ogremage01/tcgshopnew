package com.shop.scheduler.image.source.scryfall;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Scryfall Cards API 클라이언트.
 * {@code /cards/{set}/{number}/{lang}} 은 "All other methods" 한도(10/s, 100ms)를 따른다.
 */
@Component
@Slf4j
public class ScryfallCardClient {

    static final long MIN_INTERVAL_MS = 100L;
    static final long RATE_LIMIT_WAIT_MS = 30_000L;
    private static final int WEBCLIENT_MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;
    private static final String USER_AGENT = "NewTechShop/1.0 (scryfall-image-fallback)";
    private static final String ACCEPT = "application/json;q=0.9,*/*;q=0.8";

    private final WebClient webClient = WebClient.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs()
                    .maxInMemorySize(WEBCLIENT_MAX_IN_MEMORY_BYTES))
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .defaultHeader(HttpHeaders.ACCEPT, ACCEPT)
            .build();
    private final Object rateLock = new Object();
    private long nextAllowedAtMs;

    public Optional<ScryfallCardImages> fetchCardImages(String set, String collectorNumber) {
        if (isBlank(set) || isBlank(collectorNumber)) {
            return Optional.empty();
        }
        ScryfallCardResponse response = fetchCard(set.trim().toLowerCase(Locale.ROOT), collectorNumber.trim(), true);
        ScryfallCardImages images = extract(response);
        return Optional.ofNullable(images);
    }

    public byte[] downloadImage(String imageUrl) {
        if (isBlank(imageUrl)) {
            return null;
        }
        try {
            return webClient.get().uri(imageUrl).exchangeToMono(res -> {
                if (!res.statusCode().is2xxSuccessful()) {
                    log.warn("Scryfall image HTTP non-success. status={}, url={}", res.statusCode().value(), imageUrl);
                    return Mono.empty();
                }
                String contentType = res.headers().contentType().map(Object::toString).orElse("");
                if (!contentType.startsWith("image")) {
                    log.warn("Scryfall image invalid content-type. contentType={}, url={}", contentType, imageUrl);
                    return Mono.empty();
                }
                return res.bodyToMono(byte[].class);
            }).block();
        } catch (Exception e) {
            log.warn("Scryfall image download failed. url={}, msg={}", imageUrl, e.getMessage());
            return null;
        }
    }

    private ScryfallCardResponse fetchCard(String set, String collectorNumber, boolean retryOn429) {
        throttle();
        try {
            return webClient.get()
                    .uri("https://api.scryfall.com/cards/{set}/{number}", set, collectorNumber)
                    .exchangeToMono(res -> {
                        if (res.statusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                            return Mono.error(new ScryfallRateLimitException());
                        }
                        if (res.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                            log.warn("Scryfall card not found. set={}, number={}", set, collectorNumber);
                            return Mono.empty();
                        }
                        if (!res.statusCode().is2xxSuccessful()) {
                            log.warn("Scryfall card HTTP non-success. status={}, set={}, number={}",
                                    res.statusCode().value(), set, collectorNumber);
                            return Mono.empty();
                        }
                        return res.bodyToMono(ScryfallCardResponse.class);
                    })
                    .block();
        } catch (ScryfallRateLimitException e) {
            log.warn("Scryfall HTTP 429. waiting {}ms then retry. set={}, number={}",
                    RATE_LIMIT_WAIT_MS, set, collectorNumber);
            sleepQuiet(RATE_LIMIT_WAIT_MS);
            if (retryOn429) {
                return fetchCard(set, collectorNumber, false);
            }
            log.warn("Scryfall still rate-limited after wait. set={}, number={}", set, collectorNumber);
            return null;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                log.warn("Scryfall HTTP 429. waiting {}ms then retry. set={}, number={}",
                        RATE_LIMIT_WAIT_MS, set, collectorNumber);
                sleepQuiet(RATE_LIMIT_WAIT_MS);
                if (retryOn429) {
                    return fetchCard(set, collectorNumber, false);
                }
            }
            log.warn("Scryfall card request failed. set={}, number={}, msg={}",
                    set, collectorNumber, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Scryfall card request failed. set={}, number={}, msg={}",
                    set, collectorNumber, e.getMessage());
            return null;
        }
    }

    static ScryfallCardImages extract(ScryfallCardResponse response) {
        if (response == null) {
            return null;
        }
        String front = normalUrl(response.imageUris());
        String back = null;
        List<ScryfallCardFace> faces = response.cardFaces();
        if (faces != null && !faces.isEmpty()) {
            if (isBlank(front)) {
                front = normalUrl(faces.get(0) == null ? null : faces.get(0).imageUris());
            }
            if (faces.size() > 1) {
                back = normalUrl(faces.get(1) == null ? null : faces.get(1).imageUris());
            }
        }
        if (isBlank(front)) {
            return null;
        }
        return new ScryfallCardImages(front, isBlank(back) ? null : back);
    }

    private static String normalUrl(ScryfallImageUris imageUris) {
        if (imageUris == null) {
            return null;
        }
        return imageUris.normal();
    }

    private void throttle() {
        synchronized (rateLock) {
            long wait = nextAllowedAtMs - System.currentTimeMillis();
            if (wait > 0) {
                sleepQuiet(wait);
            }
            nextAllowedAtMs = System.currentTimeMillis() + MIN_INTERVAL_MS;
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Scryfall rate-limit wait interrupted", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ScryfallCardImages(String frontNormalUrl, String backNormalUrl) {
        public boolean hasFront() {
            return frontNormalUrl != null && !frontNormalUrl.isBlank();
        }

        public boolean hasBack() {
            return backNormalUrl != null && !backNormalUrl.isBlank();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScryfallCardResponse(
            @JsonProperty("image_uris") ScryfallImageUris imageUris,
            @JsonProperty("card_faces") List<ScryfallCardFace> cardFaces) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScryfallCardFace(
            @JsonProperty("image_uris") ScryfallImageUris imageUris) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScryfallImageUris(String normal) {
    }

    static final class ScryfallRateLimitException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
