package com.shop.scheduler.image.source.scryfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient.ScryfallCardFace;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient.ScryfallCardImages;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient.ScryfallCardResponse;
import com.shop.scheduler.image.source.scryfall.ScryfallCardClient.ScryfallImageUris;

class ScryfallCardClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("최상위 image_uris.normal을 앞면으로 사용한다")
    void extract_usesTopLevelNormalUri() {
        ScryfallCardResponse response = new ScryfallCardResponse(
                new ScryfallImageUris("https://cards.scryfall.io/normal/front.jpg"), null);
        ScryfallCardImages images = ScryfallCardClient.extract(response);
        assertThat(images).isNotNull();
        assertThat(images.frontNormalUrl()).isEqualTo("https://cards.scryfall.io/normal/front.jpg");
        assertThat(images.hasBack()).isFalse();
    }

    @Test
    @DisplayName("최상위 image_uris가 없으면 card_faces의 normal을 앞/뒷면으로 사용한다")
    void extract_usesCardFacesWhenNoTopLevelUris() {
        ScryfallCardResponse response = new ScryfallCardResponse(null, List.of(
                new ScryfallCardFace(new ScryfallImageUris("https://example.com/front.jpg")),
                new ScryfallCardFace(new ScryfallImageUris("https://example.com/back.jpg"))));
        ScryfallCardImages images = ScryfallCardClient.extract(response);
        assertThat(images).isNotNull();
        assertThat(images.frontNormalUrl()).isEqualTo("https://example.com/front.jpg");
        assertThat(images.backNormalUrl()).isEqualTo("https://example.com/back.jpg");
    }

    @Test
    @DisplayName("Scryfall JSON의 image_uris.normal을 파싱한다")
    void parse_imageUrisNormalFromJson() throws Exception {
        String json = """
                {
                  "object": "card",
                  "image_uris": {
                    "small": "https://example.com/small.jpg",
                    "normal": "https://example.com/normal.jpg",
                    "png": "https://example.com/large.png"
                  }
                }
                """;
        ScryfallCardResponse response = objectMapper.readValue(json, ScryfallCardResponse.class);
        ScryfallCardImages images = ScryfallCardClient.extract(response);
        assertThat(images.frontNormalUrl()).isEqualTo("https://example.com/normal.jpg");
    }
}
