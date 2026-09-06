package com.shop.product.mapper;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductImageUrlResolver {

    public static final String DEFAULT_CARD_IMAGE_URL = "/card-images/default-card.png";
    private static final String CARD_IMAGES_PREFIX = "/card-images/";

    @Value("${file.path.card-images}")
    private String cardImagesBasePath;

    public String resolveImageUrl(String imageSource, String game, String imageCodeOrUrl) {
        return ensureLocalCardImageOrDefault(resolveImageUrlInternal(imageSource, game, imageCodeOrUrl));
    }

    public String resolveImageUrlBack(String imageSource, String game, String imageCodeOrUrl) {
        return ensureLocalCardImageOrDefault(resolveImageUrlBackInternal(imageSource, game, imageCodeOrUrl));
    }

    /**
     * {@code /card-images/**} 로컬 경로는 디스크에 파일이 있을 때만 반환하고, 없으면 기본 카드 이미지 URL을 반환한다.
     */
    public String ensureLocalCardImageOrDefault(String publicUrl) {
        if (!isNotBlank(publicUrl)) {
            return DEFAULT_CARD_IMAGE_URL;
        }
        if (DEFAULT_CARD_IMAGE_URL.equals(publicUrl)) {
            return publicUrl;
        }
        if (!publicUrl.startsWith(CARD_IMAGES_PREFIX)) {
            return publicUrl;
        }
        String relative = URLDecoder.decode(
                publicUrl.substring(CARD_IMAGES_PREFIX.length()),
                StandardCharsets.UTF_8);
        Path filePath = Path.of(normalizeBasePath(cardImagesBasePath), relative);
        if (Files.isRegularFile(filePath)) {
            return publicUrl;
        }
        return DEFAULT_CARD_IMAGE_URL;
    }

    private String resolveImageUrlInternal(String imageSource, String game, String imageCodeOrUrl) {
        if ("TCGP".equalsIgnoreCase(imageSource) && isNotBlank(game) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/" + urlEncode(game) + "/" + urlEncode(imageCodeOrUrl) + ".jpg";
        }
        if ("OPB".equalsIgnoreCase(imageSource) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/mtg-kr/" + gameSlug(game) + "/"
                    + normalizeOpbImageKey(game, imageCodeOrUrl) + "-en.png";
        }
        if ("SCRYFALL".equalsIgnoreCase(imageSource) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/mtg-kr/" + gameSlug(game) + "/"
                    + normalizeOpbImageKey(game, imageCodeOrUrl) + "-en.jpg";
        }
        if (isNotBlank(imageCodeOrUrl)) {
            return imageCodeOrUrl;
        }
        log.info("resolveImageUrlInternal: imageSource={}, game={}, imageCodeOrUrl={}", imageSource, game, imageCodeOrUrl);
        return "";
    }

    private String resolveImageUrlBackInternal(String imageSource, String game, String imageCodeOrUrl) {
        if ("OPB".equalsIgnoreCase(imageSource) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/mtg-kr/" + gameSlug(game) + "/"
                    + normalizeOpbImageKey(game, imageCodeOrUrl) + "-enback.png";
        }
        if ("SCRYFALL".equalsIgnoreCase(imageSource) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/mtg-kr/" + gameSlug(game) + "/"
                    + normalizeOpbImageKey(game, imageCodeOrUrl) + "-enback.jpg";
        }
        if ("TCGP".equalsIgnoreCase(imageSource) && isNotBlank(game) && isNotBlank(imageCodeOrUrl)) {
            return "/card-images/" + urlEncode(game) + "/" + urlEncode(imageCodeOrUrl) + "_1" + ".jpg";
        }
        if (isNotBlank(imageCodeOrUrl)) {
            return imageCodeOrUrl;
        }
        return "";
    }

    private static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "";
        }
        return basePath.endsWith("/") || basePath.endsWith("\\")
                ? basePath.substring(0, basePath.length() - 1)
                : basePath;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String gameSlug(String game) {
        if ("Flesh & Blood TCG".equalsIgnoreCase(game)) {
            return "fab";
        }
        if ("Magic: The Gathering".equalsIgnoreCase(game) || "Magic".equalsIgnoreCase(game)) {
            return "mtg";
        }
        return "unknown";
    }

    private String normalizeOpbImageKey(String game, String imageCodeOrUrl) {
        if ("mtg".equals(gameSlug(game))) {
            return imageCodeOrUrl;
        }
        return imageCodeOrUrl;
    }
}
